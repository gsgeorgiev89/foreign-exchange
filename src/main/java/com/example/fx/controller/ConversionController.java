package com.example.fx.controller;

import com.example.fx.domain.Conversion;
import com.example.fx.dto.ConversionRequest;
import com.example.fx.dto.ConversionResponse;
import com.example.fx.exception.FxException;
import com.example.fx.repository.ConversionRepository;
import com.example.fx.service.ConversionService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversions")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;
    private final ConversionRepository conversionRepository;

    @PostMapping
    public ResponseEntity<ConversionResponse> convert(
            @RequestBody @Valid ConversionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        ConversionResponse response = conversionService.convert(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ConversionResponse>> getConversions(
            @RequestParam(required = false) UUID transactionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false) String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (transactionId == null && date == null && clientId == null) {
            throw new FxException(
                FxException.ErrorCode.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST,
                "At least one filter must be provided: transactionId, date, or clientId"
            );
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Conversion> conversions;

        if (transactionId != null) {
            conversions = conversionRepository.findById(transactionId)
                    .map(c -> new PageImpl<>(List.of(c), pageable, 1))
                    .orElse(new PageImpl<>(List.of(), pageable, 0));
        } else if (clientId != null && date != null) {
            Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            conversions = conversionRepository.findByClientIdAndCreatedAtBetween(clientId, from, to, pageable);
        } else if (clientId != null) {
            conversions = conversionRepository.findByClientId(clientId, pageable);
        } else {
            Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            conversions = conversionRepository.findByCreatedAtBetween(from, to, pageable);
        }

        Page<ConversionResponse> response = conversions.map(this::toResponse);
        return ResponseEntity.ok(response);
    }

    private ConversionResponse toResponse(Conversion conversion) {
        return new ConversionResponse(
            conversion.getId(),
            conversion.getSourceAmount(),
            conversion.getFromCurrency(),
            conversion.getTargetAmount(),
            conversion.getToCurrency(),
            conversion.getRate(),
            conversion.getCreatedAt(),
            List.of()
        );
    }
}