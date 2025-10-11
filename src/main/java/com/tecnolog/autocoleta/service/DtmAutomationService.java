package com.tecnolog.autocoleta.service;

import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DtmAutomationService {

    private static final Logger log = LoggerFactory.getLogger(DtmAutomationService.class);

    private final DtmRepository dtmRepository;
    private final DtmLockRepository lockRepository;
    private final DtmProcessingService processingService;

    public DtmAutomationService(DtmRepository dtmRepository,
                              DtmLockRepository lockRepository,
                              DtmProcessingService processingService) {
        this.dtmRepository = dtmRepository;
        this.lockRepository = lockRepository;
        this.processingService = processingService;
    }

    @Transactional // Usará o bean "transactionManager" por convenção
    public Map<String, Integer> processBatch(int limit) {
        List<DtmPendingRow> pendentes = dtmRepository.buscarPendentesOrdenado(limit);
        if (pendentes == null || pendentes.isEmpty()) {
            return Map.of("sucessos", 0, "falhas", 0);
        }

        int successCount = 0;
        int failureCount = 0;

        for (DtmPendingRow row : pendentes) {
            // Tenta obter o lock para o DTM. Se falhar, outro processo já o pegou.
            if (!lockRepository.tryLock(row.getIdDtm())) {
                continue;
            }

            try {
                processingService.processarDtm(row);
                successCount++;
            } catch (Exception e) {
                // A exceção já foi tratada e logada em DtmProcessingService.
                // Aqui apenas contamos a falha para o resumo do lote.
                log.warn("Falha ao processar DTM {} no lote. Causa: {}", row.getIdDtm(), e.getMessage());
                failureCount++;
            }
        }

        if (successCount > 0 || failureCount > 0) {
            log.info("Batch concluído. Sucesso: {}, Falhas: {}.", successCount, failureCount);
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("sucessos", successCount);
        result.put("falhas", failureCount);
        return result;
    }
}