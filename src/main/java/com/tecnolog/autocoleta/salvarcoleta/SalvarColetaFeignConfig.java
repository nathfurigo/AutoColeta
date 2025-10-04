package com.tecnolog.autocoleta.salvarcoleta;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SalvarColetaFeignConfig {

  @Bean
  public RequestInterceptor salvarColetaAuth(
      @Value("${app.salvarColeta.tokenHash}") String token) {

    final String auth = "Hash " + token;

    return tmpl -> {
      tmpl.header("Authorization", auth);          // mesmo esquema do portal
      tmpl.header("Hash", token);                  // redundância que alguns handlers esperam
      tmpl.header("Accept", "application/json");
      tmpl.header("Content-Type", "application/json; charset=UTF-8");
      tmpl.header("Accept-Language", "pt-BR");
      tmpl.header("User-Agent", "autocoleta/1.0");
    };
  }
}
