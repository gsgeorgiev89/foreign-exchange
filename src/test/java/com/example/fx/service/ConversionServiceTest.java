package com.example.fx.service;

import com.example.fx.domain.Balance;
import com.example.fx.domain.Conversion;
import com.example.fx.dto.ConversionRequest;
import com.example.fx.dto.ConversionResponse;
import com.example.fx.exception.FxException;
import com.example.fx.provider.ExchangeRateProvider;
import com.example.fx.repository.BalanceRepository;
import com.example.fx.repository.ClientRepository;
import com.example.fx.repository.ConversionRepository;
import com.example.fx.repository.IdempotencyKeyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private BalanceRepository balanceRepository;
    @Mock private ConversionRepository conversionRepository;
    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock private ExchangeRateProvider exchangeRateProvider;

    @InjectMocks
    private ConversionService conversionService;

    @Test
    void convert_insufficientFunds_throwsFxException() {
        ConversionRequest request = new ConversionRequest(
            "CLIENT-001", "USD", "EUR", new BigDecimal("999999.00")
        );

        when(clientRepository.existsById("CLIENT-001")).thenReturn(true);
        when(exchangeRateProvider.getRates("USD"))
            .thenReturn(Map.of("EUR", new BigDecimal("0.86")));

        Balance balance = new Balance();
        balance.setClientId("CLIENT-001");
        balance.setCurrency("USD");
        balance.setAmount(new BigDecimal("100.00"));

        when(balanceRepository.findByClientIdAndCurrencyForUpdate("CLIENT-001", "USD"))
            .thenReturn(Optional.of(balance));

        FxException ex = assertThrows(FxException.class,
            () -> conversionService.convert(request, null));

        assertThat(ex.getErrorCode()).isEqualTo(FxException.ErrorCode.INSUFFICIENT_FUNDS);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void convert_clientNotFound_throwsFxException() {
        ConversionRequest request = new ConversionRequest(
            "INVALID", "USD", "EUR", new BigDecimal("100.00")
        );

        when(clientRepository.existsById("INVALID")).thenReturn(false);

        FxException ex = assertThrows(FxException.class,
            () -> conversionService.convert(request, null));

        assertThat(ex.getErrorCode()).isEqualTo(FxException.ErrorCode.CLIENT_NOT_FOUND);
    }

    @Test
    void convert_happyPath_debitsSourceAndCreditsTarget() {
        ConversionRequest request = new ConversionRequest(
            "CLIENT-001", "USD", "EUR", new BigDecimal("100.00")
        );

        when(clientRepository.existsById("CLIENT-001")).thenReturn(true);
        when(exchangeRateProvider.getRates("USD"))
            .thenReturn(Map.of("EUR", new BigDecimal("0.86")));

        Balance sourceBalance = new Balance();
        sourceBalance.setClientId("CLIENT-001");
        sourceBalance.setCurrency("USD");
        sourceBalance.setAmount(new BigDecimal("1000.00"));

        Balance targetBalance = new Balance();
        targetBalance.setClientId("CLIENT-001");
        targetBalance.setCurrency("EUR");
        targetBalance.setAmount(new BigDecimal("500.00"));

        when(balanceRepository.findByClientIdAndCurrencyForUpdate("CLIENT-001", "USD"))
            .thenReturn(Optional.of(sourceBalance));
        when(balanceRepository.findByClientIdAndCurrencyForUpdate("CLIENT-001", "EUR"))
            .thenReturn(Optional.of(targetBalance));
        when(balanceRepository.findAllByClientId("CLIENT-001"))
            .thenReturn(List.of(sourceBalance, targetBalance));

        Conversion savedConversion = new Conversion();
        savedConversion.setId(UUID.randomUUID());
        savedConversion.setClientId("CLIENT-001");
        savedConversion.setFromCurrency("USD");
        savedConversion.setToCurrency("EUR");
        savedConversion.setSourceAmount(new BigDecimal("100.00"));
        savedConversion.setTargetAmount(new BigDecimal("86.0000"));
        savedConversion.setRate(new BigDecimal("0.86"));
        savedConversion.setCreatedAt(Instant.now());
        when(conversionRepository.save(any())).thenReturn(savedConversion);

        ConversionResponse response = conversionService.convert(request, null);

        assertThat(sourceBalance.getAmount()).isEqualByComparingTo("900.00");
        assertThat(targetBalance.getAmount()).isEqualByComparingTo("586.00");
        assertThat(response.targetAmount()).isEqualByComparingTo("86.0000");
    }
}