package com.example.fx.controller;

import com.example.fx.dto.RateResponse;
import com.example.fx.exception.ExchangeRateUnavailableException;
import com.example.fx.provider.ExchangeRateProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rates")
@RequiredArgsConstructor
public class RateController {

    private final ExchangeRateProvider exchangeRateProvider;

    @GetMapping
    public ResponseEntity<RateResponse> getRate(
            @RequestParam @NotBlank @Pattern(regexp = "[A-Z]{3}") String from,
            @RequestParam @NotBlank @Pattern(regexp = "[A-Z]{3}") String to) {

        Map<String, BigDecimal> rates = exchangeRateProvider.getRates(from);

        if (!rates.containsKey(to)) {
            throw new ExchangeRateUnavailableException(
                "No rate available for " + from + "/" + to, null);
        }

        return ResponseEntity.ok(new RateResponse(from, to, rates.get(to)));
    }
}
