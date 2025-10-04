package com.tecnolog.autocoleta.pedidos;

import com.tecnolog.autocoleta.dtm.DtmJson; // <-- este
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "pedidosApi",
        url = "${app.pedidos.baseUrl}",               // ex.: https://synclog.com.br
        configuration = PedidosFeignConfig.class
)
public interface PedidosApiClient {

    // Ajuste o path conforme o seu endpoint real:
    // se baseUrl == https://synclog.com.br -> value = "/api/Pedidos/v1/Novo"
    // se baseUrl já inclui /api/Pedidos/v1 -> value = "/Novo"
    @PostMapping(value = "/api/Pedidos/v1/Novo",
                 consumes = "application/json",
                 produces = "application/json")
    Map<String, Object> inserir(@RequestBody com.tecnolog.autocoleta.dtm.DtmJson payload);
}
