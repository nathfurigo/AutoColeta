package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse; // <- você moveu para .dto, então importe daqui
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SalvarColetaFeignAdapter implements SalvarColetaClient {

    private final SalvarColetaFeign feign;
    private final String tokenHash;

    public SalvarColetaFeignAdapter(SalvarColetaFeign feign,
                                    @Value("${app.salvarColeta.tokenHash}") String tokenHash) {
        this.feign = feign;
        this.tokenHash = tokenHash;
    }

    @Override
    public SalvarColetaResponse salvar(SalvaColetaModel model) {
        if (model.getTokenHash() == null || model.getTokenHash().trim().isEmpty()) {
            model.setTokenHash(tokenHash);
        }
        return feign.salvar(model);
    }
}
