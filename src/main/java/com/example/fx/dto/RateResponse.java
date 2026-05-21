package com.example.fx.dto;

import java.math.BigDecimal;

public record RateResponse(
    String from,
    String to,
    BigDecimal rate
) {}