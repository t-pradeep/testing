package com.test.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class YamlConfig {
    
    @Bean
    public ObjectMapper yamlObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.factory(new YAMLFactory()).build();
    }
}
