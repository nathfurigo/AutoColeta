package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "salvarColeta",
    url = "${app.salvarColeta.base-url}",
    configuration = SalvarColetaFeignConfig.class
)
public interface SalvarColetaClient {

    @PostMapping(
        value = "/api/v1/PedidoColeta/SalvaColeta",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    SalvarColetaResponse salvar(@RequestBody SalvaColetaModel body);
}
