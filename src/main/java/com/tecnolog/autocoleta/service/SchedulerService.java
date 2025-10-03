package com.tecnolog.autocoleta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final DtmAutomationService service;

    // Lê o tamanho do lote de application.properties/yml, default = 5
    private final int batchSize;

    public SchedulerService(
            DtmAutomationService service,
            @Value("${app.scheduler.batch-size:5}") int batchSize
    ) {
        this.service = service;
        this.batchSize = batchSize;
    }

    // Executa após cada intervalo (em ms). Default = 15000 (15s) se a prop não existir.
    @Scheduled(fixedDelayString = "${app.scheduler.fixed-delay-ms:30000}")
    public void run() {
        try {
            int ok = service.processBatch(batchSize);
            if (ok > 0) {
                log.info("Scheduler processou {} DTM(s).", ok);
            } else {
                log.debug("Scheduler executou e não havia DTM pendente.");
            }
        } catch (Exception e) {
            log.error("Falha ao executar scheduler: {}", e.getMessage(), e);
        }
    }
}
