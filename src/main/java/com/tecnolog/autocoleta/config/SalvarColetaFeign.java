package com.tecnolog.autocoleta.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvarColetaResponse;

@FeignClient(name = "salvarColeta", configuration = SalvarColetaFeignConfig.class)
public interface SalvarColetaFeign {

    @PostMapping(value = "/api/v1/PedidoColeta/SalvaColeta",
                 consumes = "application/json",
                 produces = "application/json")
    SalvarColetaResponse salvar(@RequestBody SalvaColetaModel body);
    
}