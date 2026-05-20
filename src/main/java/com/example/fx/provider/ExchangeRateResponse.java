package com.example.fx.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateResponse(
    String result,
    @JsonProperty("base_code") String baseCode,
    Map<String, BigDecimal> rates
) {}