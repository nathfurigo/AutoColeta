package com.tecnolog.autocoleta.web;

import com.tecnolog.autocoleta.service.DtmAutomationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/automation/coletas")
public class AutomationController {

    private final DtmAutomationService service;

    public AutomationController(DtmAutomationService service) {
        this.service = service;
    }

    @RequestMapping(
        value = "/run-batch",
        method = { RequestMethod.POST, RequestMethod.GET },
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> runBatch(@RequestParam(defaultValue = "100") int limit) {
        if (limit < 1) {
            limit = 1;
        }

        Map<String, Object> body = new HashMap<>();
        try {
            Map<String, Integer> result = service.processBatch(limit);
            int sucessos = result.getOrDefault("sucessos", 0);
            int falhas = result.getOrDefault("falhas", 0);

            body.put("erro", Boolean.FALSE);
            body.put("dtmsProcessadas", sucessos + falhas);
            body.put("sucessos", sucessos);
            body.put("falhas", falhas);
            body.put("mensagem", String.format("Lote concluído. %d com sucesso, %d com falha.", sucessos, falhas));
            
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("erro", Boolean.TRUE);
            body.put("mensagem", "Erro geral na execução do lote: " + e.getMessage());
            return ResponseEntity.internalServerError().body(body);
        }
    }
}