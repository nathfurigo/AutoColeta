package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.dto.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;


public interface SalvarColetaClient {
    
    SalvarColetaResponse salvar(SalvaColetaModel body);

    void adicionarOcorrencia(long idDtm, String numeroColeta);
    
    void tryAdicionarOcorrenciaTokenOnly(long idDtm); 
}