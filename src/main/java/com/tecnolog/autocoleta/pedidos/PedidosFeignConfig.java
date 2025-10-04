package com.tecnolog.autocoleta.pedidos;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PedidosFeignConfig {

  @Bean
  public Request.Options feignOptions(
      @Value("${app.pedidos.connect-timeout-ms:5000}") int connectTimeout,
      @Value("${app.pedidos.read-timeout-ms:30000}") int readTimeout
  ) {
    return new Request.Options(connectTimeout, readTimeout);
  }

  @Bean Logger.Level feignLoggerLevel() { return Logger.Level.FULL; }
  @Bean ErrorDecoder pedidosErrorDecoder() { return new PedidosErrorDecoder(); }

  @Bean
  RequestInterceptor pedidosAuthInterceptor(
      @Value("${app.pedidos.auth.header:}") String header,
      @Value("${app.pedidos.auth.value:}")  String value) {
    return template -> {
      if (!header.isBlank() && !value.isBlank()) {
        template.header(header, value);
        template.header("Authorization", header + " " + value);
      }
      // evita gzip para o logger conseguir imprimir o HTML
      template.header("Accept-Encoding", "identity");

      template.header("Content-Type", "application/json; charset=UTF-8");
      template.header("Accept", "application/json, */*");
      template.header("Accept-Language", "pt-BR");
      template.header("User-Agent", "autocoleta/1.0");
    };
  }
}
