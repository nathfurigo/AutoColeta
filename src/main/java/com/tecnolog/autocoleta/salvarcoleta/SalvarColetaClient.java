package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.dto.SalvarColetaRequest;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;

public interface SalvarColetaClient {
    SalvarColetaResponse salvar(SalvarColetaRequest body);
}