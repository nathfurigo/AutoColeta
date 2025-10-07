package com.tecnolog.autocoleta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final DtmAutomationService automationService;

    @Value("${app.scheduler.batch-size:100}")
    private int batchSize;

    public SchedulerService(DtmAutomationService automationService) {
        this.automationService = automationService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.fixed-delay-ms:30000}")
    public void run() {
        log.info("Scheduler iniciado para processar lote de DTMs...");
        try {
            Map<String, Integer> result = automationService.processBatch(batchSize);

            int sucessos = result.getOrDefault("sucessos", 0);
            int falhas = result.getOrDefault("falhas", 0);
            int totalProcessado = sucessos + falhas;

            if (totalProcessado > 0) {
                log.info("Batch do scheduler concluído. DTMs processadas: {}. Sucessos: {}, Falhas: {}.", totalProcessado, sucessos, falhas);
            } else {
                log.debug("Scheduler executou e não havia DTMs pendentes para processar.");
            }
        } catch (Exception e) {
            log.error("Falha crítica ao executar o batch do scheduler. A transação pode ter sido revertida.", e);
        }
    }
}