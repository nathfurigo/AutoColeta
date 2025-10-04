package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;

public interface SalvarColetaClient {
    SalvarColetaResponse salvar(SalvaColetaModel body);
}
