package com.tecnolog.autocoleta.pedidos;

import com.tecnolog.autocoleta.pedidos.payload.DtmJson;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(
    name = "pedidosApi",
    url = "${app.pedidos.base-url}",
    configuration = PedidosFeignConfig.class
)
public interface PedidosApiClient {

  @PostMapping(value = "/api/Pedidos/v1/Novo",
               consumes = "application/json", produces = "application/json")
  Map<String, Object> inserir(@RequestBody DtmJson payload);

  @PostMapping(value = "/api/Pedidos/v1/Novo/Lote",
               consumes = "application/json", produces = "application/json")
  List<Map<String, Object>> inserirLote(@RequestBody List<DtmJson> payload);
}
