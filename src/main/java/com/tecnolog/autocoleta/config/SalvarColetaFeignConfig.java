package com.tecnolog.autocoleta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.RequestInterceptor;

@Configuration
public class SalvarColetaFeignConfig {

    @Value("${app.salvarColeta.tokenHash}")
    private String tokenHash;

    @Bean
    public RequestInterceptor salvarColetaHashHeaderInterceptor() {
        return template -> {
            if (!template.headers().containsKey("Hash")) {
                template.header("Hash", tokenHash);
            }
        };
    }
}
