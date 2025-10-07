package com.tecnolog.autocoleta.config;

import org.springframework.context.annotation.Bean;
import com.tecnolog.autocoleta.config.AppProperties; // Importe a classe de propriedades
import feign.RequestInterceptor;

public class SalvarColetaFeignConfig {

    @Bean
    public RequestInterceptor salvarColetaHashHeaderInterceptor(AppProperties appProperties) {
        return template -> {
            if (!template.headers().containsKey("Hash")) {
                String tokenHash = appProperties.getSalvarColeta().getTokenHash();
                if (tokenHash != null && !tokenHash.isBlank()) {
                    template.header("Hash", tokenHash.trim());
                }
            }
        };
    }
}