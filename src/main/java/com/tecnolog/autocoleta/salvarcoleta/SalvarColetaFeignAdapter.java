package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("prod")
public class SalvarColetaFeignAdapter implements SalvarColetaClient {

    private final SalvarColetaFeign feign;

    public SalvarColetaFeignAdapter(SalvarColetaFeign feign) {
        this.feign = feign;
    }

    @Override
    public Map<String, Object> salvar(SalvaColetaModel body) {
        return feign.salvar(body);
    }
}
