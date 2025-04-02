package com.test.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

// Remove @Configuration here
@ConfigurationProperties(prefix = "gitlab.api")
@Data
@Validated
public class GitLabApiProperties {

    @NotBlank(message = "GitLab API base URL must be configured")
    private String baseUrl;

    @NotBlank(message = "GitLab API private token must be configured")
    private String privateToken;

}
