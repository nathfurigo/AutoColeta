package com.tecnolog.autocoleta.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SynclogFeignConfig implements RequestInterceptor {

    @Value("${app.salvarColeta.idGuid:}")
    private String idGuid;

    @Override
    public void apply(RequestTemplate template) {
        if (idGuid != null && !idGuid.trim().isEmpty()) {
            template.header("id_guid", idGuid);
        }
        template.header("Accept", "application/json");
        template.header("Content-Type", "application/json");
    }
}
