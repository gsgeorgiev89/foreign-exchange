package com.example.fx.controller;

import com.example.fx.dto.BalanceDto;
import com.example.fx.dto.ConversionRequest;
import com.example.fx.dto.ConversionResponse;
import com.example.fx.dto.ErrorResponse;
import com.example.fx.repository.ConversionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConversionControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ConversionRepository conversionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterAll
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM idempotency_keys");
        jdbcTemplate.execute("DELETE FROM conversions");
        jdbcTemplate.execute("UPDATE balances SET amount = 10000.0000 WHERE client_id = 'CLIENT-001' AND currency = 'USD'");
        jdbcTemplate.execute("UPDATE balances SET amount = 8000.0000 WHERE client_id = 'CLIENT-001' AND currency = 'EUR'");
        jdbcTemplate.execute("UPDATE balances SET amount = 5000.0000 WHERE client_id = 'CLIENT-002' AND currency = 'GBP'");
    }

    // 1. Happy path
    @Test
    void convert_happyPath_debitsAndCreditsBalances() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);

        ConversionRequest request = new ConversionRequest(
            "CLIENT-001", "USD", "EUR", new BigDecimal("100.00")
        );

        ResponseEntity<ConversionResponse> response = restTemplate.exchange(
            "/conversions",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            ConversionResponse.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().transactionId()).isNotNull();
        assertThat(response.getBody().sourceAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getBody().sourceCurrency()).isEqualTo("USD");

        List<BalanceDto> updatedBalances = response.getBody().updatedBalances();
        BigDecimal newUsd = updatedBalances.stream()
            .filter(b -> b.currency().equals("USD"))
            .findFirst()
            .orElseThrow()
            .amount();

        assertThat(newUsd).isLessThan(new BigDecimal("10000.0000"));
    }

    @Test
    void convert_insufficientFunds_returns422() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ConversionRequest request = new ConversionRequest(
            "CLIENT-001", "USD", "EUR", new BigDecimal("999999.00")
        );

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/conversions",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void convert_idempotencyReplay_returnsSameResult() {
        String idempotencyKey = UUID.randomUUID().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ConversionRequest request = new ConversionRequest(
            "CLIENT-001", "USD", "EUR", new BigDecimal("50.00")
        );

        long countBefore = conversionRepository.findByClientId(
            "CLIENT-001", PageRequest.of(0, 100)
        ).getTotalElements();

        ResponseEntity<ConversionResponse> first = restTemplate.exchange(
            "/conversions", HttpMethod.POST,
            new HttpEntity<>(request, headers), ConversionResponse.class
        );

        ResponseEntity<ConversionResponse> second = restTemplate.exchange(
            "/conversions", HttpMethod.POST,
            new HttpEntity<>(request, headers), ConversionResponse.class
        );

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().transactionId())
            .isEqualTo(second.getBody().transactionId());

        long countAfter = conversionRepository.findByClientId(
            "CLIENT-001", PageRequest.of(0, 100)
        ).getTotalElements();
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    void convert_clientNotFound_returns404() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ConversionRequest request = new ConversionRequest(
            "INVALID-CLIENT", "USD", "EUR", new BigDecimal("100.00")
        );

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
            "/conversions",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("CLIENT_NOT_FOUND");
    }
}