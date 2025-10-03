package com.tecnolog.autocoleta.service;

import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmMapper;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmRepository;
import com.tecnolog.autocoleta.pedidos.PedidosApiClient;
import com.tecnolog.autocoleta.pedidos.payload.DtmJson;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DtmAutomationService {

    // ===== DEPENDÊNCIAS INJETADAS =====
    private final DtmRepository dtmRepository;
    private final DtmLockRepository lockRepository;
    private final DtmMapper mapper;
    private final PedidosApiClient pedidosApi;

    @Transactional
    public Optional<Long> processOne() {
        Optional<DtmPendingRow> opt = dtmRepository.fetchNextPending();
        if (opt.isEmpty()) {
            log.info("Nenhuma DTM pendente encontrada.");
            return Optional.empty();
        }

        DtmPendingRow row = opt.get();
        long id = row.getIdDtm();

        try {
            if (!lockRepository.tryLock(id)) {
                log.info("DTM {} já está em processamento por outro worker.", id);
                return Optional.empty();
            }

            DtmJson payload = mapper.toPedidos(row);
            Map<String, Object> resp;
            try {
                resp = pedidosApi.inserir(payload);
            } catch (FeignException fe) {
                String body;
                try { body = fe.contentUTF8(); } catch (Throwable t) { body = fe.getMessage(); }
                String msg = "Erro HTTP na API Pedidos: status=" + fe.status() + " body=" + body;
                lockRepository.markError(id, msg);
                throw new RuntimeException(msg, fe);
            }

            if (resp == null) {
                lockRepository.markError(id, "Resposta nula da API Pedidos");
                throw new RuntimeException("Resposta nula da API Pedidos");
            }

            if (parseErro(resp)) {
                String msg = parseMensagem(resp);
                lockRepository.markError(id, msg);
                throw new RuntimeException("Falha Pedidos: " + msg);
            }

            lockRepository.markProcessed(id);
            log.info("Pedido criado com sucesso para DTM {}. Response: {}", id, resp);
            return Optional.of(id);

        } catch (Exception e) {
            try {
                lockRepository.markError(id, e.getMessage());
            } catch (Exception ignored) {
                log.debug("Falha ao marcar erro no lock {}", id, ignored);
            }
            log.error("Erro ao processar DTM {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /** Processa em lote (retorna quantas foram OK). */
    @Transactional
    public int processBatch(int limit) {
        List<DtmPendingRow> pendentes = dtmRepository.buscarPendentesOrdenado(limit);
        int ok = 0;
        for (DtmPendingRow r : pendentes) {
            long id = r.getIdDtm();
            if (!lockRepository.tryLock(id)) {
                continue;
            }
            try {
                DtmJson payload = mapper.toPedidos(r);
                Map<String, Object> resp = pedidosApi.inserir(payload);
                if (resp == null) {
                    lockRepository.markError(id, "Resposta nula da API Pedidos");
                    log.warn("DTM {} falhou: resposta nula", id);
                    continue;
                }
                if (parseErro(resp)) {
                    String msg = parseMensagem(resp);
                    lockRepository.markError(id, "API retornou erro: " + msg);
                    log.warn("DTM {} falhou: {}", id, msg);
                    continue;
                }
                lockRepository.markProcessed(id);
                ok++;
                log.info("DTM {} inserida com sucesso: {}", id, parseMensagem(resp));
            } catch (FeignException e) {
                String body;
                try { body = e.contentUTF8(); } catch (Throwable t) { body = e.getMessage(); }
                lockRepository.markError(id, "HTTP " + e.status() + " - " + body);
            } catch (Exception e) {
                lockRepository.markError(id, e.getMessage());
            }
        }
        return ok;
    }

    // ===== Helpers API Pedidos =====
    private boolean parseErro(Map<String, Object> resp) {
        if (resp == null) return true;
        Object raw = resp.get("erro");
        if (raw == null) raw = resp.get("error");
        if (raw == null) return false;
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) return false;
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private String parseMensagem(Map<String, Object> resp) {
        if (resp == null) return "Resposta nula da API";
        Object msg = resp.get("mensagem");
        if (msg == null) msg = resp.get("message");
        if (msg == null) msg = resp.get("response");
        return (msg != null) ? String.valueOf(msg) : String.valueOf(resp);
    }
}
