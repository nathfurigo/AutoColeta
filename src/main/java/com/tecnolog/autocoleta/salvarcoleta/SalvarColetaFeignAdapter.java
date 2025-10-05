// com.tecnolog.autocoleta.salvarcoleta.SalvarColetaFeignAdapter
package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.dto.SalvarColetaRequest;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
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
    public SalvarColetaResponse salvar(SalvarColetaRequest body) {
        if (body.getTokenHash() == null || body.getTokenHash().isBlank()) {
            body.setTokenHash(tokenHash);
        }
        return feign.salvar(body);
    }
}
