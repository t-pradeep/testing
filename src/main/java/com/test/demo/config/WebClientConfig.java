package com.test.demo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(GitLabApiProperties.class)
public class WebClientConfig {

    private final GitLabApiProperties properties;

    @Bean(name = "gitlabWebClient")
    public WebClient gitlabWebClient(WebClient.Builder webClientBuilder) {
        if (!StringUtils.hasText(properties.getBaseUrl()) ||
            !StringUtils.hasText(properties.getPrivateToken())) {
            log.error("GitLab API URL or Token is not configured.");
            throw new IllegalStateException("GitLab API credentials must be configured.");
        }

        log.info("Configuring WebClient for GitLab API: {}", properties.getBaseUrl());

        return webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader("PRIVATE-TOKEN", properties.getPrivateToken())
                // Removed debug logging filters
                .build();
    }

    // Removed logRequest() and logResponse() methods
}
