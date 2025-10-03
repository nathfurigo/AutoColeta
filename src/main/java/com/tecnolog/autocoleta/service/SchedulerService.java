package com.tecnolog.autocoleta.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final DtmAutomationService service;

    @Scheduled(fixedDelayString = "${app.scheduler.fixed-delay-ms:15000}")
    public void run() {
        int ok = service.processBatch(5);
        if (ok > 0) log.info("Scheduler processou {} DTM(s).", ok);
    }
}
