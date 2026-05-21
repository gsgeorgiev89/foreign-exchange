package com.example.fx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ConversionRequest(
    @NotBlank String clientId,
    @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a valid currency code") String fromCurrency,
    @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a valid currency code") String toCurrency,
    @NotNull @Positive BigDecimal amount
) {}