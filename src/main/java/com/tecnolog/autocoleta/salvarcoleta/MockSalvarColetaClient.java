package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@Profile("mock")
public class MockSalvarColetaClient implements SalvarColetaClient {

    private static final Logger LOG = LoggerFactory.getLogger(MockSalvarColetaClient.class);

    @Override
    public SalvarColetaResponse salvar(SalvaColetaModel body) {
        long fakeId = ThreadLocalRandom.current().nextLong(100000, 999999);
        LOG.info("[MOCK] SalvarColeta chamado. Gerando coleta fake={}", fakeId);

        SalvarColetaResponse resp = new SalvarColetaResponse();
        resp.setErro(false);
        resp.setResponse(String.valueOf(fakeId));
        return resp;
    }
}
