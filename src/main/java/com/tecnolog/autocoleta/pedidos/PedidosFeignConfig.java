package com.tecnolog.autocoleta.pedidos;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class PedidosFeignConfig {
  @Bean
  public Request.Options feignOptions(
      @Value("${app.pedidos.connect-timeout-ms:5000}") int connectTimeout,
      @Value("${app.pedidos.read-timeout-ms:30000}") int readTimeout) {
    return new Request.Options(connectTimeout, readTimeout);
  }

  @Bean Logger.Level feignLoggerLevel() { return Logger.Level.FULL; }
  @Bean ErrorDecoder pedidosErrorDecoder() { return new PedidosErrorDecoder(); }

  @Bean
  public RequestInterceptor pedidosAuthInterceptor(
      @Value("${app.pedidos.security.value}") String token) {
    return template -> {
      template.headers().remove("Authorization");
      template.headers().remove("Hash");
      template.header("Authorization", "Hash " + token);

      template.header("Accept-Encoding", "identity");
      template.header("Content-Type", "application/json; charset=UTF-8");
      template.header("Accept", "application/json, */*");
      template.header("Accept-Language", "pt-BR");
      template.header("User-Agent", "autocoleta/1.0");
    };
  }
  @Bean
  public RequestInterceptor authHeader(
      @Value("${app.pedidos.auth.header:Hash}") String authHeaderName,
      @Value("${app.pedidos.auth.value}") String token
  ) {
    return template -> {
      template.headers().remove("Authorization");
      template.headers().remove("Hash");
      template.header(authHeaderName, token);
      template.header("Authorization", "Hash " + token);
      template.header("Accept-Encoding", "identity");
      template.header("Content-Type", "application/json; charset=UTF-8");
      template.header("Accept", "application/json, */*");
      template.header("Accept-Language", "pt-BR");
      template.header("User-Agent", "autocoleta/1.0");
    };
  }
}
