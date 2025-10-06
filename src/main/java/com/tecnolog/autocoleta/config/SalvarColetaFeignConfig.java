package com.tecnolog.autocoleta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.RequestInterceptor;

@Configuration
public class SalvarColetaFeignConfig {

    @Value("${app.salvarColeta.tokenHash}")
    private String tokenHash;

    /**
     * Intercepta TODAS as chamadas do Feign desta interface e injeta o header exigido pelo SyncLog:
     *   Hash: <TokenHash>
     */
    @Bean
    public RequestInterceptor salvarColetaHashHeaderInterceptor() {
        return template -> {
            // evita sobrescrever se já vier de outro ponto
            if (!template.headers().containsKey("Hash")) {
                template.header("Hash", tokenHash);
            }
        };
    }
}
