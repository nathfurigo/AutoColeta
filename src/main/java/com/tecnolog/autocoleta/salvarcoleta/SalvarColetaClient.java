package com.tecnolog.autocoleta.salvarcoleta;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.tecnolog.autocoleta.config.SynclogFeignConfig;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;

@FeignClient(
    name = "salvarColetaFeign",
    url = "${app.salvarColeta.baseUrl}",
    configuration = SynclogFeignConfig.class
)
public interface SalvarColetaClient {

    @PostMapping("/api/v1/PedidoColeta/SalvaColeta")
    SalvarColetaResponse salvar(@RequestBody SalvaColetaModel body);
}
