package com.example.fx.dto;

import java.math.BigDecimal;

public record BalanceDto(
    String currency,
    BigDecimal amount
) {}