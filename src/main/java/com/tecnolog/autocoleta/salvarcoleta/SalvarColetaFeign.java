package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "salvarColeta", url = "${app.salvarColeta.baseUrl}")
public interface SalvarColetaFeign {
    @PostMapping(value = "/api/v1/PedidoColeta/SalvaColeta", consumes = "application/json", produces = "application/json")
    Map<String, Object> salvar(@RequestBody SalvaColetaModel payload);
}
