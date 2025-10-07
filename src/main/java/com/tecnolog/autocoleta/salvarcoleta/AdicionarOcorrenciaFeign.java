package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.dto.SyncLogOcorrenciaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "adicionarOcorrencia")
public interface AdicionarOcorrenciaFeign {

    @PostMapping(value = "/api/v1/AddOcorrencia",
                 consumes = "application/json",
                 produces = "application/json")
    SyncLogOcorrenciaResponse adicionarOcorrencia(@RequestBody AddOcorrenciaRequest body);
}