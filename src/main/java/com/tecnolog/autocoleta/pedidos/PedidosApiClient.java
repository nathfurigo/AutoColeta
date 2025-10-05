package com.tecnolog.autocoleta.pedidos;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
  name = "pedidos",
  url = "${app.pedidos.base-url}",
  configuration = com.tecnolog.autocoleta.pedidos.PedidosFeignConfig.class
)
public interface PedidosApiClient {

    @PostMapping(value = "/api/Pedidos/v1/Novo",
                 consumes = "application/json",
                 produces = "application/json")
    Map<String, Object> inserir(@RequestBody com.tecnolog.autocoleta.pedidos.payload.DtmJson payload);
  }
