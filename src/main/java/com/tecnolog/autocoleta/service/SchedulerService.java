package com.tecnolog.autocoleta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final DtmAutomationService automationService;

    @Value("${app.scheduler.batch-size:5}")
    private int batchSize;

    public SchedulerService(DtmAutomationService automationService) {
        this.automationService = automationService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.fixed-delay-ms:30000}")
    public void run() {
        try {
            int ok = automationService.processBatch(batchSize);
            if (ok > 0) {
                log.info("Batch concluído. {} DTM(s) processadas com sucesso.", ok);
            } else {
                log.debug("Scheduler executou e não havia DTM pendente ou todas estavam travadas.");
            }
        } catch (Exception e) {
            log.error("Falha ao executar batch do scheduler", e);
        }
    }
}
