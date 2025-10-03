package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import java.util.Map;

public interface SalvarColetaClient {
    Map<String, Object> salvar(SalvaColetaModel body);
}
