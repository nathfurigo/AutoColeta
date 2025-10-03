package com.tecnolog.autocoleta.pedidos;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

public class PedidosFeignConfig {

  @Bean
  public Request.Options feignOptions(
      @Value("${app.pedidos.connect-timeout-ms:5000}") int connectTimeout,
      @Value("${app.pedidos.read-timeout-ms:30000}") int readTimeout
  ) {
    return new Request.Options(connectTimeout, readTimeout);
  }
}
