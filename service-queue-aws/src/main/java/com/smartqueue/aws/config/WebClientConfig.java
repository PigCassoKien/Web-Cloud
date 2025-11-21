package com.smartqueue.aws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {
    
    @Value("${service.eta.base-url}")
    private String etaServiceBaseUrl;
    
    @Value("${service.eta.timeout:5000}")
    private int timeoutMillis;
    
    @Bean
    public WebClient etaServiceWebClient() {
        return WebClient.builder()
                .baseUrl(etaServiceBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }
    
    @Bean
    public Duration etaServiceTimeout() {
        return Duration.ofMillis(timeoutMillis);
    }
}