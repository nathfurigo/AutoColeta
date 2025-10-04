package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.dto.SalvarColetaRequest;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class SalvarColetaFeignAdapter implements SalvarColetaClient {

    private final SalvarColetaFeign feign;

    @Value("${app.salvarColeta.tokenHash}")
    private String tokenHash;

    public SalvarColetaFeignAdapter(SalvarColetaFeign feign) {
        this.feign = feign;
    }

    @Override
    public SalvarColetaResponse salvar(SalvaColetaModel body) {
        SalvarColetaRequest req = toRequest(body, tokenHash);
        return feign.salvar(req);
    }

    private SalvarColetaRequest toRequest(SalvaColetaModel model, String token) {
        SalvarColetaRequest req = new SalvarColetaRequest();
        // Copia campos com mesmo nome/tipo
        BeanUtils.copyProperties(model, req);
        // Token no corpo (se o contrato da API exige)
        req.setTokenHash(token);

        return req;
    }
}
