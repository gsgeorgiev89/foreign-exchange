package com.example.fx.provider;

import com.example.fx.exception.ExchangeRateUnavailableException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateProvider.class);

    private final RestClient restClient;

    public ExchangeRateProvider(RestClient.Builder builder,
                               @Value("${fx.provider.base-url}") String baseUrl,
                               @Value("${fx.provider.timeout-seconds:5}") int timeoutSeconds) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(clientHttpRequestFactory(timeoutSeconds))
                .build();
    }

    @Cacheable(value = "rates", key = "#from")
    public Map<String, BigDecimal> getRates(String from) {
        log.info("Fetching rates from provider for base: {}", from);
        try {
            ExchangeRateResponse response = restClient.get()
                .uri("/latest/{from}", from)
                .retrieve()
                .body(ExchangeRateResponse.class);

            if (response == null
                || !"success".equals(response.result())
                || response.rates() == null) {
                throw new ExchangeRateUnavailableException(
                    "No rates found for base " + from, null);
            }

            return response.rates();

        } catch (ExchangeRateUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ExchangeRateUnavailableException(
                "Exchange rate provider unavailable for base " + from, e);
        }
    }

    private ClientHttpRequestFactory clientHttpRequestFactory(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return factory;
    }
}