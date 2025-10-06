package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.dto.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;

public interface SalvarColetaClient {
    SalvarColetaResponse salvar(SalvaColetaModel body);
}