package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.dto.SalvarColetaResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tecnolog.autocoleta.dto.SalvaColetaModel;

@FeignClient(name = "salvarColeta", url = "${app.salvarColeta.baseUrl}")
public interface SalvarColetaFeign {

    @PostMapping(value = "/api/v1/PedidoColeta/SalvaColeta",
                 consumes = "application/json",
                 produces = "application/json")
    SalvarColetaResponse salvar(@RequestBody SalvaColetaModel body);
}
