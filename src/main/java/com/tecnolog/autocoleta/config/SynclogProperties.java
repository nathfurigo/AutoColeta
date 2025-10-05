package com.tecnolog.autocoleta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "synclog")
public class SynclogProperties {

    private String baseUrl;
    private String idGuid;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getIdGuid() {
        return idGuid;
    }

    public void setIdGuid(String idGuid) {
        this.idGuid = idGuid;
    }
}
