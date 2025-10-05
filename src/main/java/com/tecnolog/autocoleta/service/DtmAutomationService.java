package com.tecnolog.autocoleta.service;

import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmRepository;
import com.tecnolog.autocoleta.dtm.DtmToSalvarColetaMapper;
import com.tecnolog.autocoleta.salvarcoleta.SalvarColetaClient;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;

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
    private final DtmToSalvarColetaMapper mapper;
    private final SalvarColetaClient salvarColeta;

    // <<< construtor explícito (sem @RequiredArgsConstructor) >>>
    public DtmAutomationService(DtmRepository dtmRepository,
                                DtmLockRepository lockRepository,
                                DtmToSalvarColetaMapper mapper,
                                SalvarColetaClient salvarColeta) {
        this.dtmRepository = dtmRepository;
        this.lockRepository = lockRepository;
        this.mapper = mapper;
        this.salvarColeta = salvarColeta;
    }

    /** Processa 1 DTM (útil para ser chamado pelo Scheduler) */
    @Transactional
    public boolean processOne() {
        List<DtmPendingRow> pend = dtmRepository.buscarPendentesOrdenado(1);
        if (pend == null || pend.isEmpty()) return false;

        DtmPendingRow row = pend.get(0);
        long id = row.getIdDtm();

        if (!lockRepository.tryLock(id)) {
            log.info("DTM {} já está travada por outro worker", id);
            return false;
        }

        try {
            SalvaColetaModel model = mapper.map(row);
            SalvarColetaResponse resp = salvarColeta.salvar(model);

            if (resp == null) {
                lockRepository.markError(id, "Resposta nula da API Pedidos");
                throw new IllegalStateException("Resposta nula da API Pedidos");
            }
            if (resp.isErro()) {
                String msg = String.valueOf(resp.getResponse());
                lockRepository.markError(id, "API retornou erro: " + msg);
                log.warn("DTM {} falhou: {}", id, msg);
                return false;
            }

            lockRepository.markProcessed(id);
            log.info("DTM {} inserida com sucesso. Resp: {}", id, String.valueOf(resp.getResponse()));
            return true;

        } catch (FeignException fe) {
            // contentUTF8 nem sempre existe dependendo da versão do Feign
            String body;
            try { body = fe.getMessage(); } catch (Throwable t) { body = "FeignException sem mensagem"; }
            String msg = "HTTP " + fe.status() + " ao chamar SalvarColeta: " + body;
            lockRepository.markError(id, msg);
            throw new RuntimeException(msg, fe);

        } catch (RuntimeException e) {
            // já marcamos o erro acima quando aplicável
            throw e;
        }
    }

    /** Processa em lote (com lock e marcação processed/erro) */
    @Transactional
    public int processBatch(int limit) {
        List<DtmPendingRow> pendentes = dtmRepository.buscarPendentesOrdenado(limit);
        if (pendentes == null || pendentes.isEmpty()) return 0;

        int ok = 0;
        for (DtmPendingRow r : pendentes) {
            long id = r.getIdDtm();
            if (!lockRepository.tryLock(id)) continue;

            try {
                SalvaColetaModel model = mapper.map(r);
                SalvarColetaResponse resp = salvarColeta.salvar(model);

                if (resp == null) {
                    lockRepository.markError(id, "Resposta nula da API Pedidos");
                    continue;
                }
                if (resp.isErro()) {
                    lockRepository.markError(id, "API erro: " + resp.getResponse());
                    continue;
                }

                lockRepository.markProcessed(id);
                ok++;

            } catch (FeignException fe) {
                String body;
                try { body = fe.getMessage(); } catch (Throwable t) { body = "FeignException sem mensagem"; }
                lockRepository.markError(id, "HTTP " + fe.status() + ": " + body);

            } catch (Throwable t) {
                lockRepository.markError(id, "Falha inesperada: " + t.getMessage());
            }
        }
        return ok;
    }
}
