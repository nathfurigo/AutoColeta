package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse; 

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tecnolog.autocoleta.dto.SalvarColetaRequest;

@Component
public class SalvarColetaFeignAdapter implements SalvarColetaClient {
    private final SalvarColetaFeign feign;
    private final String tokenHash;

    public SalvarColetaFeignAdapter(SalvarColetaFeign feign,
                                    @Value("${app.salvarColeta.tokenHash}") String tokenHash) {
        this.feign = feign;
        this.tokenHash = tokenHash;
    }

    public SalvarColetaResponse salvar(SalvaColetaModel model) {
        if (model.getTokenHash() == null || model.getTokenHash().trim().isEmpty()) {
            model.setTokenHash(tokenHash);
        }
        return feign.salvar(model);
    }

    public String salvar(SalvarColetaRequest body) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
