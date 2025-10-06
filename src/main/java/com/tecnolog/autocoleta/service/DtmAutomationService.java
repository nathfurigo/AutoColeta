package com.tecnolog.autocoleta.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmRepository;
import com.tecnolog.autocoleta.dtm.DtmToSalvaColetaMapper;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
import com.tecnolog.autocoleta.salvarcoleta.SalvarColetaClient;
import com.tecnolog.autocoleta.dto.SalvaColetaModel;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DtmAutomationService {

    private static final Logger log = LoggerFactory.getLogger(DtmAutomationService.class);

    private final DtmRepository dtmRepository;
    private final DtmLockRepository lockRepository;
    private final DtmToSalvaColetaMapper mapper;
    private final SalvarColetaClient salvarColeta;
    private final ObjectMapper om;

    public DtmAutomationService(DtmRepository dtmRepository,
                              DtmLockRepository lockRepository,
                              DtmToSalvaColetaMapper mapper,
                              SalvarColetaClient salvarColeta,
                              ObjectMapper om) {
        this.dtmRepository = dtmRepository;
        this.lockRepository = lockRepository;
        this.mapper = mapper;
        this.salvarColeta = salvarColeta;
        this.om = om;
    }

    @Transactional
    public boolean processOne() {
        List<DtmPendingRow> pendentes = dtmRepository.buscarPendentesOrdenado(1);
        if (pendentes == null || pendentes.isEmpty()) {
            return false;
        }

        DtmPendingRow row = pendentes.get(0);
        long dtmId = row.getIdDtm();

        if (!lockRepository.tryLock(dtmId)) {
            log.warn("DTM {} já está travada, não será processada pelo run-once.", dtmId);
            return false; 
        }
        
        try {
            executeProcessing(row);
            return true;
        } catch (Exception e) {
            log.error("Falha ao processar DTM {} via run-once: {}", dtmId, e.getMessage());
            throw e; 
        }
    }

    @Transactional
    public int processBatch(int limit) {
        List<DtmPendingRow> pendentes = dtmRepository.buscarPendentesOrdenado(limit);
        if (pendentes == null || pendentes.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        int failureCount = 0;

        for (DtmPendingRow row : pendentes) {
            if (!lockRepository.tryLock(row.getIdDtm())) {
                continue; 
            }

            try {
                executeProcessing(row);
                successCount++;
            } catch (Exception e) {
                failureCount++;
            }
        }

        if (successCount > 0 || failureCount > 0) {
            log.info("Batch concluído. Sucesso: {}, Falhas: {}.", successCount, failureCount);
        }
        return successCount;
    }

    private void executeProcessing(DtmPendingRow row) {
        long dtmId = row.getIdDtm();
        try {
            SalvaColetaModel requestPayload = mapper.map(row);
            if (log.isDebugEnabled()) {
                log.debug("Payload para API (DTM {}): {}", dtmId, safeJson(requestPayload));
            }

            SalvarColetaResponse response = salvarColeta.salvar(requestPayload);
            log.info("API Response Object State - erro: {}, response: '{}'", response.isErro(), response.getResponse());

            if (response == null) {
                throw new IllegalStateException("Resposta nula da API SalvarColeta");
            }
            if (response.isErro()) {
                throw new IllegalStateException("API retornou erro: " + response.getResponse());
            }

            lockRepository.markProcessed(dtmId);
            log.info("DTM {} processada com sucesso. Resposta da API: {}", dtmId, response.getResponse());

        } catch (FeignException fe) {
            String errorBody = fe.contentUTF8() != null ? fe.contentUTF8() : "FeignException sem corpo de resposta.";
            String errorMessage = "Erro HTTP " + fe.status() + " ao chamar SalvarColeta: " + errorBody;
            lockRepository.markError(dtmId, errorMessage);
            throw new RuntimeException(errorMessage, fe);
        } catch (Throwable t) {
            String errorMessage = "Falha inesperada no processamento: " + t.getMessage();
            lockRepository.markError(dtmId, errorMessage);
            throw new RuntimeException(errorMessage, t);
        }
    }

    private String safeJson(Object obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "<json-error:" + e.getMessage() + ">";
        }
    }
}