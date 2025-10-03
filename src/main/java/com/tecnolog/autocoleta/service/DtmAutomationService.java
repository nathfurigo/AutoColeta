package com.tecnolog.autocoleta.service;

import com.tecnolog.autocoleta.config.AppProperties;
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

import java.util.Map;
import java.util.Optional;

import lombok.Data;

@Service
@Data
@Slf4j
public class DtmAutomationService {

    private DtmRepository dtmRepository;
    private DtmLockRepository lockRepository;
    private DtmMapper mapper;
    private PedidosApiClient pedidosApi;
    private AppProperties props;

    @Transactional
    public Optional<Long> processOne() {
        Optional<DtmPendingRow> opt = dtmRepository.fetchNextPending();
        if (!opt.isPresent()) { 
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

            boolean hasError = parseErro(resp);
            if (hasError) {
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
            throw e;
        }
    }

    @Transactional
    public int processBatch(int limit) {
        int ok = 0;
        for (int i = 0; i < limit; i++) {
            Optional<Long> res = processOne();
            if (!res.isPresent()) break;
            ok++;
        }
        return ok;
    }

    @Transactional
    public void processarPendentes() {
java.util.List<DtmPendingRow> pendentes = dtmRepository.buscarPendentesOrdenado();

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
                log.info("DTM {} inserida com sucesso: {}", id, parseMensagem(resp));

            } catch (FeignException e) {
                String body;
                try { body = e.contentUTF8(); } catch (Throwable t) { body = e.getMessage(); }
                lockRepository.markError(id, "HTTP " + e.status() + " - " + body);
                log.error("Erro Feign ao enviar DTM {}: {} ({})", id, body, e.status(), e);
            } catch (Exception e) {
                lockRepository.markError(id, e.getMessage());
                log.error("Erro ao processar DTM {}: {}", id, e.getMessage(), e);
            }
        }
    }

    private boolean parseErro(Map<String, Object> resp) {
        if (resp == null) return false;
        Object raw = resp.containsKey("erro") ? resp.get("erro") : resp.get("error");
        if (raw == null) return false;

        if (raw instanceof Boolean) {
            return ((Boolean) raw).booleanValue();
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue() != 0;
        }

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
