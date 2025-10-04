package com.tecnolog.autocoleta.pedidos;

import feign.Response;
import feign.codec.ErrorDecoder;
import feign.FeignException;
import feign.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Collection;

public class PedidosErrorDecoder implements ErrorDecoder {
  private static final Logger log = LoggerFactory.getLogger(PedidosErrorDecoder.class);

  @Override
  public Exception decode(String methodKey, Response response) {
    String body = "";
    try {
      if (response.body() != null) {
        Charset cs = Util.UTF_8;
        // tenta extrair charset do header
        Collection<String> cts = response.headers().getOrDefault("content-type", response.headers().getOrDefault("Content-Type", java.util.Set.of()));
        if (!cts.isEmpty()) {
          String ct = cts.iterator().next();
          int i = ct.toLowerCase().indexOf("charset=");
          if (i >= 0) {
            String enc = ct.substring(i + 8).trim();
            try { cs = Charset.forName(enc); } catch (Exception ignore) {}
          }
        }
        body = Util.toString(response.body().asReader(cs));
      }
    } catch (Exception e) {
      log.warn("Falha lendo corpo de erro Feign", e);
    }
    log.error("[{}] HTTP {} {}. Corpo de erro:\n{}", methodKey, response.status(), response.reason(), body);
    return FeignException.errorStatus(methodKey, response);
  }
}
