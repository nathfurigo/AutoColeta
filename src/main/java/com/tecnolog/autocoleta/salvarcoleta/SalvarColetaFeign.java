package com.tecnolog.autocoleta.salvarcoleta;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tecnolog.autocoleta.config.SalvarColetaFeignConfig;
import com.tecnolog.autocoleta.dto.SalvarColetaRequest;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;

@FeignClient(
    name = "salvarColeta",
    url  = "${app.salvarColeta.baseUrl}",
    configuration = SalvarColetaFeignConfig.class
)
public interface SalvarColetaFeign {

    @PostMapping(value = "/api/v1/PedidoColeta/SalvaColeta",
                 consumes = "application/json",
                 produces = "application/json")
    SalvarColetaResponse salvar(@RequestBody SalvarColetaRequest body);
}
