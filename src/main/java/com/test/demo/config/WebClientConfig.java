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
        if (!StringUtils.hasText(properties.getCredentials().getUrl()) || 
            !StringUtils.hasText(properties.getCredentials().getToken())) {
            log.error("GitLab API URL or Token is not configured.");
            throw new IllegalStateException("GitLab API credentials must be configured.");
        }

        log.info("Configuring WebClient for GitLab API: {}", properties.getCredentials().getUrl());

        return webClientBuilder
                .baseUrl(properties.getCredentials().getUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader("PRIVATE-TOKEN", properties.getCredentials().getToken())
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("GitLab API Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("PRIVATE-TOKEN")) {
                    values.forEach(value -> log.debug("  {}: {}", name, value));
                } else {
                    log.debug("  {}: *****", name);
                }
            });
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("GitLab API Response Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
