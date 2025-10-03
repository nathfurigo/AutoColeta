package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Profile("mock")
public class MockSalvarColetaClient implements SalvarColetaClient {

    private static final Logger LOG = LoggerFactory.getLogger(MockSalvarColetaClient.class);

    @Override
    public Map<String, Object> salvar(SalvaColetaModel body) {
        long fakeId = ThreadLocalRandom.current().nextLong(100000, 999999);
        LOG.info("[MOCK] SalvarColeta chamado. Gerando coleta fake={}", fakeId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("erro", false);
        resp.put("response", String.valueOf(fakeId));
        return resp;
    }
}
