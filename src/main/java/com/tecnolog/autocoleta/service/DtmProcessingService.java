package com.tecnolog.autocoleta.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.salvarcoleta.SalvarColetaClient;
import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmToSalvaColetaMapper;
import com.tecnolog.autocoleta.dto.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
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

    public DtmProcessingService(
            SalvarColetaClient salvarColetaClient,
            DtmToSalvaColetaMapper dtmMapper,
            DtmLockRepository dtmLockRepository,
            ObjectMapper objectMapper
    ) {
        this.salvarColetaClient = salvarColetaClient;
        this.dtmMapper = dtmMapper;
        this.dtmLockRepository = dtmLockRepository;
        this.objectMapper = objectMapper;
    }

    public void processarDtm(DtmPendingRow dtmRow) {
        long idDtm = dtmRow.getIdDtm();
        String numeroColetaGerada = null;

        try {
            SalvaColetaModel requestPayload = dtmMapper.map(dtmRow);
            log.debug("Payload para API SalvarColeta (DTM {}): {}", idDtm, safeJson(requestPayload));

            SalvarColetaResponse response = salvarColetaClient.salvar(requestPayload);
            log.info("Resposta da API SalvarColeta para DTM {}: erro={}, response='{}'", idDtm, response.isErro(), response.getResponse());

            if (response == null || response.isErro()) {
                throw new IllegalStateException("API SalvarColeta retornou erro: " + (response != null ? response.getResponse() : "Resposta nula"));
            }

            numeroColetaGerada = response.getResponse();
            log.info("DTM {} - Coleta {} gerada com sucesso. Tentando registrar ocorrência...", idDtm, numeroColetaGerada);

            salvarColetaClient.adicionarOcorrencia(idDtm, numeroColetaGerada);
            dtmLockRepository.markProcessed(idDtm, numeroColetaGerada);
            log.info("DTM {} - Ocorrência para coleta {} registrada com sucesso.", idDtm, numeroColetaGerada);

        } catch (Exception e) {
            String errorMessage;
            if (numeroColetaGerada != null) {
                errorMessage = String.format("CRÍTICO: Coleta %s foi criada para a DTM %d, mas o registro da ocorrência falhou. Causa: %s",
                        numeroColetaGerada, idDtm, e.getMessage());
            } else {
                errorMessage = "Falha ao processar DTM " + idDtm + ". Causa: " + e.getMessage();
            }

            log.error(errorMessage);
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