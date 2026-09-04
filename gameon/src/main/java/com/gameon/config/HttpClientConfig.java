package com.gameon.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Provides a shared {@link RestTemplate} used to call external, keyless public APIs:
 * <ul>
 *   <li>Open-Meteo forecast API (weather)</li>
 *   <li>OpenStreetMap Nominatim (venue search)</li>
 * </ul>
 * Timeouts are kept short so a slow third-party never blocks a request thread for long;
 * callers degrade gracefully (weather "unavailable", empty venue list) on failure.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate externalApiRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(12))
                .build();
    }
}
