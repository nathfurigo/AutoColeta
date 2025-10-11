package com.tecnolog.autocoleta.config;

import com.tecnolog.autocoleta.dto.AddOcorrenciaRequest;
import com.tecnolog.autocoleta.dto.SyncLogOcorrenciaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "adicionarOcorrencia",
    url = "${app.salvarOcorrencia.baseUrl}"   // <-- URL fixa via property
)
public interface AdicionarOcorrenciaFeign {

    @PostMapping(
        value = "/api/v1/AddOcorrencia",
        consumes = "application/json",
        produces = "application/json"
    )
    SyncLogOcorrenciaResponse adicionarOcorrencia(@RequestBody AddOcorrenciaRequest body);
}
