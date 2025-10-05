package com.tecnolog.autocoleta.web;

import com.tecnolog.autocoleta.service.DtmAutomationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/automation/coletas")
public class AutomationController {

    private final DtmAutomationService service;

    // <<< injeção via construtor (sem Lombok) >>>
    public AutomationController(DtmAutomationService service) {
        this.service = service;
    }

    /** Executa UMA criação de coleta (tenta processar 1 DTM pendente). */
    @PostMapping("/run-once")
    public ResponseEntity<Map<String, Object>> runOnce() {
        Map<String, Object> body = new HashMap<String, Object>();
        try {
            boolean ok = service.processOne();
            if (ok) {
                body.put("erro", Boolean.FALSE);
                body.put("mensagem", "1 DTM processada.");
            } else {
                body.put("erro", Boolean.FALSE);
                body.put("mensagem", "Sem pendências ou DTM travada.");
            }
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("erro", Boolean.TRUE);
            body.put("mensagem", e.getMessage());
            return ResponseEntity.internalServerError().body(body);
        }
    }

    /** Executa em lote (até N) usando o próprio batch do service. */
    @PostMapping("/run-batch")
    public ResponseEntity<Map<String, Object>> runBatch(@RequestParam(defaultValue = "10") int limit) {
        if (limit < 1) limit = 1;

        Map<String, Object> body = new HashMap<String, Object>();
        try {
            int qtdOk = service.processBatch(limit);
            body.put("erro", Boolean.FALSE);
            body.put("qtd", qtdOk);
            body.put("mensagem", qtdOk > 0 ? "Lote concluído." : "Sem pendências.");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("erro", Boolean.TRUE);
            body.put("mensagem", e.getMessage());
            return ResponseEntity.internalServerError().body(body);
        }
    }
}
