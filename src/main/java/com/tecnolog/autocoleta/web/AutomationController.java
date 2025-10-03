package com.tecnolog.autocoleta.web;

import com.tecnolog.autocoleta.service.DtmAutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/automation/coletas")
@RequiredArgsConstructor
public class AutomationController {

    private final DtmAutomationService service;

    /** Executa UMA criação de coleta (para 1 DTM pendente). */
    @PostMapping("/run-once")
    public ResponseEntity<Map<String, Object>> runOnce() {
        try {
            Optional<Long> opt = service.processOne();
            if (opt.isPresent()) {
                Map<String, Object> ok = new HashMap<String, Object>();
                ok.put("erro", Boolean.FALSE);
                ok.put("processedIdDtm", opt.get());
                return ResponseEntity.ok(ok);
            } else {
                Map<String, Object> semPend = new HashMap<String, Object>();
                semPend.put("erro", Boolean.FALSE);
                semPend.put("mensagem", "Sem pendências");
                return ResponseEntity.ok(semPend);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("erro", Boolean.TRUE);
            err.put("mensagem", e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    /** Executa em lote (até N). */
    @PostMapping("/run-batch")
    public ResponseEntity<Map<String, Object>> runBatch(@RequestParam(defaultValue = "10") int limit) {
        if (limit < 1) limit = 1;

        List<Long> processed = new ArrayList<Long>();
        List<String> erros = new ArrayList<String>();

        for (int i = 0; i < limit; i++) {
            try {
                Optional<Long> opt = service.processOne();
                if (!opt.isPresent()) {
                    break;
                }
                processed.add(opt.get());
            } catch (Exception e) {
                erros.add(e.getMessage());
            }
        }

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("erro", !erros.isEmpty());
        resp.put("qtd", processed.size());
        resp.put("ids", processed);
        if (!erros.isEmpty()) {
            resp.put("falhas", erros);
        }

        return ResponseEntity.ok(resp);
    }
}
