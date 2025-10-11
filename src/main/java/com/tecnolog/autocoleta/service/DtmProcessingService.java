package com.tecnolog.autocoleta.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.config.SalvarColetaClient;
import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmLockStatus;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmToSalvaColetaMapper;
import com.tecnolog.autocoleta.dtm.SqlServerRepository;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvarColetaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DtmProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DtmProcessingService.class);

    private final SalvarColetaClient salvarColetaClient;
    private final DtmToSalvaColetaMapper dtmMapper;
    private final DtmLockRepository dtmLockRepository;
    private final ObjectMapper objectMapper;
    private final SqlServerRepository sqlServerRepository;

    public DtmProcessingService(
            SalvarColetaClient salvarColetaClient,
            DtmToSalvaColetaMapper dtmMapper,
            DtmLockRepository dtmLockRepository,
            ObjectMapper objectMapper,
            SqlServerRepository sqlServerRepository
    ) {
        this.salvarColetaClient = salvarColetaClient;
        this.dtmMapper = dtmMapper;
        this.dtmLockRepository = dtmLockRepository;
        this.objectMapper = objectMapper;
        this.sqlServerRepository = sqlServerRepository;
    }

    public void processarDtm(DtmPendingRow dtmRow) {
        long idDtm = dtmRow.getIdDtm();
        String numeroColetaGerada = null;
        
        DtmLockStatus initialLockStatus = dtmLockRepository.getLockStatus(idDtm);

        try {
            // Cenário de Reprocessamento: A coleta foi gerada, mas a ocorrência falhou.
            if (initialLockStatus != null && initialLockStatus.getColetaGerada() != null && !initialLockStatus.isProcessed()) {
                numeroColetaGerada = initialLockStatus.getColetaGerada();
                log.warn("DTM {} já tem coleta {} gerada, mas status está pendente. Tentando registrar ocorrência novamente...", idDtm, numeroColetaGerada);

                salvarColetaClient.adicionarOcorrencia(idDtm, numeroColetaGerada); 
                
                dtmLockRepository.markProcessed(idDtm, numeroColetaGerada);
                log.info("DTM {} - Ocorrência para coleta {} registrada com sucesso (Repro.).", idDtm, numeroColetaGerada);
                return;
            }
            
            // Mapeia o JSON da view para o objeto de requisição
            SalvaColetaModel requestPayload = dtmMapper.map(dtmRow);

            // Passo de ENRIQUECIMENTO: Busca dados faltantes no SQL Server e preenche o objeto
            sqlServerRepository.preencherDadosFaltantes(requestPayload);
            
            log.debug("Payload final para API SalvarColeta (DTM {}): {}", idDtm, safeJson(requestPayload));

            // Pré-verificação de conectividade e autenticação com a API de ocorrências
            try {
                salvarColetaClient.tryAdicionarOcorrenciaTokenOnly(idDtm);
            } catch (RuntimeException e) {
                throw new RuntimeException("Pré-requisito falhou: Não foi possível obter AccessToken para DTM " + idDtm + ". Coleta não será gerada.", e);
            }
            
            // Envia a requisição para criar a coleta
            SalvarColetaResponse response = salvarColetaClient.salvar(requestPayload);
            log.info("Resposta da API SalvarColeta para DTM {}: erro={}, response='{}'", idDtm, response.isErro(), response.getResponse());

            if (response == null || response.isErro()) {
                throw new IllegalStateException("API SalvarColeta retornou erro: " + (response != null ? response.getResponse() : "Resposta nula"));
            }

            // Se a coleta foi criada com sucesso, registra a ocorrência
            numeroColetaGerada = response.getResponse();
            log.info("DTM {} - Coleta {} gerada com sucesso. Tentando registrar ocorrência...", idDtm, numeroColetaGerada);

            salvarColetaClient.adicionarOcorrencia(idDtm, numeroColetaGerada); 
            dtmLockRepository.markProcessed(idDtm, numeroColetaGerada);
            log.info("DTM {} - Ocorrência para coleta {} registrada com sucesso.", idDtm, numeroColetaGerada);

        } catch (Exception e) {
            String errorMessage;
            if (numeroColetaGerada != null) {
                errorMessage = String.format("CRÍTICO: Coleta %s foi criada para a DTM %d, mas o registro da ocorrência final falhou. Causa: %s",
                        numeroColetaGerada, idDtm, e.getMessage());
            } else {
                errorMessage = "Falha ao processar DTM " + idDtm + ". Coleta não foi gerada. Causa: " + e.getMessage();
            }

            log.error(errorMessage, e); // Adicionado 'e' para logar o stack trace
            dtmLockRepository.markError(idDtm, errorMessage); 
            throw new RuntimeException(errorMessage, e);
        }
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "<json-error:" + e.getMessage() + ">";
        }
    }
}