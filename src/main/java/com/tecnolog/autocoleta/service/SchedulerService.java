package com.tecnolog.autocoleta.service;

import com.tecnolog.autocoleta.dtm.DtmRepository;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmToSalvarColetaMapper;
import com.tecnolog.autocoleta.salvarcoleta.SalvarColetaClient;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final DtmRepository dtmRepository;
    private final DtmToSalvarColetaMapper mapper;
    private final SalvarColetaClient salvarClient;

    public SchedulerService(DtmRepository dtmRepository,
                            DtmToSalvarColetaMapper mapper,
                            SalvarColetaClient salvarClient) {
        this.dtmRepository = dtmRepository;
        this.mapper = mapper;
        this.salvarClient = salvarClient;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.fixed-delay-ms:30000}")
    public void run() {
        List<DtmPendingRow> pend = dtmRepository.buscarPendentesOrdenado(5);
        if (pend == null || pend.isEmpty()) {
            log.debug("Scheduler executou e não havia DTM pendente.");
            return;
        }

        for (DtmPendingRow r : pend) {
            try {
                log.info("Processando DTM={} ...", r.getIdDtm());
                var body = mapper.map(r); // SalvaColetaModel
                SalvarColetaResponse resp = salvarClient.salvar(body);

                if (resp == null) {
                    log.error("Resposta nula do SalvaColeta para DTM={}", r.getIdDtm());
                    continue;
                }
                if (resp.isErro()) {
                    log.error("Falha no SalvaColeta DTM={}: {}", r.getIdDtm(), resp.getResponse());
                } else {
                    log.info("OK DTM={} -> PedidoColeta {}", r.getIdDtm(), resp.getResponse());
                    // aqui, se tiver lock-table com flag processed, marque como processado
                    // ex: dtmLockRepository.markProcessed(r.getIdDtm());
                }
            } catch (Exception ex) {
                log.error("Exceção ao processar DTM=" + r.getIdDtm(), ex);
                // dtmLockRepository.markError(r.getIdDtm(), ex.getMessage());
            }
        }
    }
}
