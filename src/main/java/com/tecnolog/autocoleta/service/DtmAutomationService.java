package com.tecnolog.autocoleta.service;

import com.tecnolog.autocoleta.config.AppProperties;
import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmMapper;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmRepository;
import com.tecnolog.autocoleta.salvarcoleta.SalvarColetaClient;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DtmAutomationService {

    private final DtmRepository dtmRepository;
    private final DtmLockRepository lockRepository;
    private final DtmMapper mapper;
    private final SalvarColetaClient salvarColetaClient;
    private final AppProperties props;

    /** Processa UMA DTM por execução */
    @Transactional
    public Optional<Long> processOne() {
        Optional<DtmPendingRow> opt = dtmRepository.fetchNextPending();
        if (!opt.isPresent()) { // <- sem Optional.isEmpty()
            log.info("Nenhuma DTM pendente encontrada.");
            return Optional.empty();
        }

        DtmPendingRow row = opt.get();
        long id = row.getIdDtm();

        try {
            // Mapear → payload
            SalvaColetaModel req = mapper.toSalvarColeta(row);

            // Garantir tokenHash (fallback das configs) — sem String.isBlank()
            if (isBlank(req.TokenHash)) {
                String cfgToken = (props.getSalvarColeta() != null)
                        ? props.getSalvarColeta().getTokenHash()
                        : null;
                if (isBlank(cfgToken)) {
                    String msg = "TokenHash ausente (payload e configuração).";
                    lockRepository.markError(id, msg);
                    throw new IllegalStateException(msg);
                }
                req.TokenHash = cfgToken;
            }

            // Chamada Feign
            Map<String, Object> resp;
            try {
                resp = salvarColetaClient.salvar(req);
            } catch (FeignException fe) {
                String body;
                try {
                    body = fe.contentUTF8();
                } catch (Throwable t) {
                    body = fe.getMessage();
                }
                String msg = "Erro HTTP na API SalvaColeta: status=" + fe.status() + " body=" + body;
                lockRepository.markError(id, msg);
                throw new RuntimeException(msg, fe);
            }

            if (resp == null) {
                lockRepository.markError(id, "Resposta nula do endpoint SalvaColeta");
                throw new RuntimeException("Resposta nula do endpoint");
            }

            boolean hasError = parseErro(resp);
            if (hasError) {
                String msg = parseMensagem(resp);
                lockRepository.markError(id, msg);
                throw new RuntimeException("Falha SalvaColeta: " + msg);
            }

            lockRepository.markProcessed(id);
            log.info("Coleta criada com sucesso para DTM {}. Response: {}", id, resp);
            return Optional.of(id);

        } catch (Exception e) {
            try {
                lockRepository.markError(id, e.getMessage());
            } catch (Exception ignored) { /* evita mascarar a causa original */ }
            log.error("Erro ao processar DTM {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /** Processa até 'limit' DTMs, parando se acabar a fila */
    @Transactional
    public int processBatch(int limit) {
        int ok = 0;
        for (int i = 0; i < limit; i++) {
            Optional<Long> res = processOne(); // <- sem var
            if (!res.isPresent()) break;       // <- sem Optional.isEmpty()
            ok++;
        }
        return ok;
    }

    /** Interpreta campo 'erro'/'error' como boolean. Aceita boolean, number, string */
    private boolean parseErro(Map<String, Object> resp) {
        if (resp == null) return false;
        Object raw = resp.containsKey("erro") ? resp.get("erro") : resp.get("error");
        if (raw == null) return false;

        // sem type pattern (Java 16+)
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

    /** Extrai mensagem padrão de sucesso/erro */
    private String parseMensagem(Map<String, Object> resp) {
        if (resp == null) return "Resposta nula da API";
        Object msg = resp.get("mensagem");
        if (msg == null) msg = resp.get("message");
        if (msg == null) msg = resp.get("response");
        return (msg != null) ? String.valueOf(msg) : String.valueOf(resp);
    }

    /** Helper para ambientes sem String.isBlank() */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
