package com.tecnolog.autocoleta.config;

import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvarColetaResponse;

public interface SalvarColetaClient {
    
    SalvarColetaResponse salvar(SalvaColetaModel body);

    void adicionarOcorrencia(long idDtm, String numeroColeta);
    
    void tryAdicionarOcorrenciaTokenOnly(long idDtm); 
}