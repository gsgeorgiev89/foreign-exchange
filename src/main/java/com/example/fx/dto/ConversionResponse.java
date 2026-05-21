package com.example.fx.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversionResponse(
    UUID transactionId,
    BigDecimal sourceAmount,
    String sourceCurrency,
    BigDecimal targetAmount,
    String targetCurrency,
    BigDecimal rate,
    Instant timestamp,
    List<BalanceDto> updatedBalances
) {}