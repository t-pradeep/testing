package com.test.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.boot.context.properties.EnableConfigurationProperties; // Import
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.util.Set; // Import

@Configuration
@EnableConfigurationProperties(WebhookProperties.class) // Enable the new properties class
public class YamlConfig {
    
    @Bean
    public ObjectMapper yamlObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.factory(new YAMLFactory()).build();
    }

    @Bean
    public MavenXpp3Reader mavenXpp3Reader() {
        return new MavenXpp3Reader();
    }

    @Bean
    public Set<String> targetBranches(WebhookProperties webhookProperties) {
        return webhookProperties.getTargetBranchesSet();
    }

    @Bean
    public Set<String> apiSpecFiles(WebhookProperties webhookProperties) {
        return webhookProperties.getApiSpecFilesSet();
    }
}
