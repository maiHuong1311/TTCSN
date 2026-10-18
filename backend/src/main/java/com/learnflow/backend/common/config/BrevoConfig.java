package com.learnflow.backend.common.config;

import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class BrevoConfig {

    @Bean("brevoRestClient")
    RestClient brevoRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return RestClient.builder().baseUrl("https://api.brevo.com/v3").requestFactory(factory).build();
    }
}
