package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class SalvarColetaFeignAdapter implements SalvarColetaClient {
    private final SalvarColetaFeign feign;

    @Override
    public Map<String, Object> salvar(SalvaColetaModel body) {
        return feign.salvar(body);
    }
}
