package com.example.fx.service;

import com.example.fx.domain.Balance;
import com.example.fx.domain.Conversion;
import com.example.fx.domain.IdempotencyKey;
import com.example.fx.dto.BalanceDto;
import com.example.fx.dto.ConversionRequest;
import com.example.fx.dto.ConversionResponse;
import com.example.fx.exception.ExchangeRateUnavailableException;
import com.example.fx.exception.FxException;
import com.example.fx.provider.ExchangeRateProvider;
import com.example.fx.repository.BalanceRepository;
import com.example.fx.repository.ClientRepository;
import com.example.fx.repository.ConversionRepository;
import com.example.fx.repository.IdempotencyKeyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversionService {

    private static final Logger log = LoggerFactory.getLogger(ConversionService.class);
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private final ClientRepository clientRepository;
    private final BalanceRepository balanceRepository;
    private final ConversionRepository conversionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ExchangeRateProvider exchangeRateProvider;

    @Transactional
    public ConversionResponse convert(ConversionRequest request, String idempotencyKey) {

        if (idempotencyKey != null) {
            Optional<IdempotencyKey> existing = idempotencyKeyRepository.findById(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyKey record = existing.get();
                if (record.getStatus() == IdempotencyKey.Status.PROCESSING) {
                    throw new FxException(
                        FxException.ErrorCode.IDEMPOTENCY_KEY_PROCESSING,
                        HttpStatus.CONFLICT,
                        "Idempotency key already processing: " + idempotencyKey
                    );
                }
                Conversion original = conversionRepository
                        .findById(record.getConversionId())
                        .orElseThrow();
                return buildResponse(original);
            }
        }

        if (!clientRepository.existsById(request.clientId())) {
            throw new FxException(
                FxException.ErrorCode.CLIENT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Client not found: " + request.clientId()
            );
        }

        Map<String, BigDecimal> rates = exchangeRateProvider.getRates(request.fromCurrency());
        if (!rates.containsKey(request.toCurrency())) {
            throw new ExchangeRateUnavailableException(
                "No rate available for " + request.fromCurrency() + "/" + request.toCurrency(), null
            );
        }
        BigDecimal rate = rates.get(request.toCurrency());

        BigDecimal targetAmount = request.amount()
                .multiply(rate)
                .setScale(SCALE, ROUNDING);

        Balance sourceBalance = balanceRepository
                .findByClientIdAndCurrencyForUpdate(request.clientId(), request.fromCurrency())
                .orElseThrow(() -> new FxException(
                    FxException.ErrorCode.BALANCE_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "Balance not found for client " + request.clientId() +
                    " currency " + request.fromCurrency()
                ));

        if (sourceBalance.getAmount().compareTo(request.amount()) < 0) {
            throw new FxException(
                FxException.ErrorCode.INSUFFICIENT_FUNDS,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Insufficient funds for client " + request.clientId() +
                " currency " + request.fromCurrency() +
                " available: " + sourceBalance.getAmount() +
                " required: " + request.amount()
            );
        }

        Balance targetBalance = balanceRepository
                .findByClientIdAndCurrencyForUpdate(request.clientId(), request.toCurrency())
                .orElseGet(() -> createBalance(request.clientId(), request.toCurrency()));

        sourceBalance.setAmount(sourceBalance.getAmount().subtract(request.amount()));
        targetBalance.setAmount(targetBalance.getAmount().add(targetAmount));
        balanceRepository.save(sourceBalance);
        balanceRepository.save(targetBalance);

        Conversion conversion = new Conversion();
        conversion.setClientId(request.clientId());
        conversion.setFromCurrency(request.fromCurrency());
        conversion.setToCurrency(request.toCurrency());
        conversion.setSourceAmount(request.amount());
        conversion.setTargetAmount(targetAmount);
        conversion.setRate(rate);
        conversionRepository.save(conversion);

        if (idempotencyKey != null) {
            IdempotencyKey record = new IdempotencyKey();
            record.setIdempotencyKey(idempotencyKey);
            record.setConversionId(conversion.getId());
            record.setStatus(IdempotencyKey.Status.COMPLETED);
            idempotencyKeyRepository.save(record);
        }

        log.info("Conversion completed: {} {} -> {} {} for client {}",
            request.amount(), request.fromCurrency(),
            targetAmount, request.toCurrency(),
            request.clientId());

        return buildResponse(conversion);
    }

    private ConversionResponse buildResponse(Conversion conversion) {
        List<Balance> balances = balanceRepository.findAllByClientId(conversion.getClientId());
        List<BalanceDto> balanceDtos = balances.stream()
                .map(b -> new BalanceDto(b.getCurrency(), b.getAmount()))
                .toList();

        return new ConversionResponse(
            conversion.getId(),
            conversion.getSourceAmount(),
            conversion.getFromCurrency(),
            conversion.getTargetAmount(),
            conversion.getToCurrency(),
            conversion.getRate(),
            conversion.getCreatedAt(),
            balanceDtos
        );
    }

    private Balance createBalance(String clientId, String currency) {
        Balance balance = new Balance();
        balance.setClientId(clientId);
        balance.setCurrency(currency);
        balance.setAmount(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
        return balance;
    }
}