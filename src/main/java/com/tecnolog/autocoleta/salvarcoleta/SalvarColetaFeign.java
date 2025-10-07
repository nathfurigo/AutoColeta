package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.config.SalvarColetaFeignConfig; // Importe a classe de configuração
import com.tecnolog.autocoleta.dto.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "salvarColeta", configuration = SalvarColetaFeignConfig.class)
public interface SalvarColetaFeign {

    @PostMapping(value = "/api/v1/PedidoColeta/SalvaColeta",
                 consumes = "application/json",
                 produces = "application/json")
    SalvarColetaResponse salvar(@RequestBody SalvaColetaModel body);
    
}