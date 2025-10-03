package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Profile("mock")
@Slf4j
public class MockSalvarColetaClient implements SalvarColetaClient {

    @Override
    public Map<String, Object> salvar(SalvaColetaModel body) {
        long fakeId = ThreadLocalRandom.current().nextLong(100000, 999999);
        log.info("[MOCK] SalvarColeta chamado. Gerando coleta fake={}", fakeId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("erro", false);                
        resp.put("response", String.valueOf(fakeId)); 
        return resp;
    }
}
