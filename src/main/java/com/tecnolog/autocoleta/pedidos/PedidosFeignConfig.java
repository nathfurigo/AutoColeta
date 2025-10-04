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

  @Bean
  feign.RequestInterceptor pedidosAuthInterceptor(
        @org.springframework.beans.factory.annotation.Value("${app.pedidos.auth.header:}") String header,
        @org.springframework.beans.factory.annotation.Value("${app.pedidos.auth.value:}")  String value) {
    return template -> {
        if (!header.isBlank() && !value.isBlank()) {
            // header original (mantém como está)
            template.header(header, value); // ex.: Hash: 5de61e...

            // extra: Authorization com o mesmo esquema usado no header
            // Se o header configurado for "Hash", ficaremos com "Authorization: Hash <valor>"
            template.header("Authorization", header + " " + value);
        }

        // garantir JSON com charset (ok se já vier de outro lugar)
        if (template.headers().getOrDefault("Content-Type", java.util.List.of()).stream()
                .noneMatch(v -> v.toLowerCase().contains("charset"))) {
            template.header("Content-Type", "application/json; charset=UTF-8");
        }

        // (opcional, ajuda em alguns gateways .NET)
        template.header("Accept", "application/json, */*");
        template.header("Accept-Language", "pt-BR");
        template.header("User-Agent", "autocoleta/1.0");
    };
  }
}
