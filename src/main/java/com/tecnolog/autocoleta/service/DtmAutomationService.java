package com.tecnolog.autocoleta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException; // ADICIONE ESTE IMPORT
import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmRepository;
import com.tecnolog.autocoleta.dtm.DtmToSalvarColetaMapper;
import com.tecnolog.autocoleta.salvarcoleta.SalvarColetaClient;
import com.tecnolog.autocoleta.dto.SalvarColetaRequest;
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
  private final ObjectMapper om;

  public DtmAutomationService(DtmRepository dtmRepository,
                              DtmLockRepository lockRepository,
                              DtmToSalvarColetaMapper mapper,
                              SalvarColetaClient salvarColeta,
                              ObjectMapper om) {
    this.dtmRepository = dtmRepository;
    this.lockRepository = lockRepository;
    this.mapper = mapper;
    this.salvarColeta = salvarColeta;
    this.om = om;
  }

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
      SalvarColetaRequest req = mapper.map(row);
      if (log.isDebugEnabled()) log.debug("Payload para API (DTM {}): {}", id, safeJson(req));

      SalvarColetaResponse resp = salvarColeta.salvar(req);

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
      String body;
      try { body = fe.getMessage(); } catch (Throwable t) { body = "FeignException sem mensagem"; }
      String msg = "HTTP " + fe.status() + " ao chamar SalvarColeta: " + body;
      lockRepository.markError(id, msg);
      throw new RuntimeException(msg, fe);

    } catch (RuntimeException e) {
      try { lockRepository.markError(id, "Falha inesperada: " + e.getMessage()); } catch (Throwable ignore) {}
      throw e;
    }
  }

  @Transactional
  public int processBatch(int limit) {
    List<DtmPendingRow> pendentes = dtmRepository.buscarPendentesOrdenado(limit);
    if (pendentes == null || pendentes.isEmpty()) return 0;

    int ok = 0, fail = 0;
    for (DtmPendingRow r : pendentes) {
      long id = r.getIdDtm();
      if (!lockRepository.tryLock(id)) continue;

      try {
        SalvarColetaRequest req = mapper.map(r);
        if (log.isDebugEnabled()) log.debug("Payload para API (DTM {}): {}", id, safeJson(req));

        SalvarColetaResponse resp = salvarColeta.salvar(req);

        if (resp == null) {
          lockRepository.markError(id, "Resposta nula da API Pedidos");
          fail++; continue;
        }
        if (resp.isErro()) {
          lockRepository.markError(id, "API erro: " + String.valueOf(resp.getResponse()));
          fail++; continue;
        }

        lockRepository.markProcessed(id);
        ok++;

      } catch (FeignException fe) {
        String body; try { body = fe.getMessage(); } catch (Throwable t) { body = "FeignException sem mensagem"; }
        lockRepository.markError(id, "HTTP " + fe.status() + ": " + body);
        fail++;
      } catch (Throwable t) {
        lockRepository.markError(id, "Falha inesperada: " + t.getMessage());
        fail++;
      }
    }
    log.info("Batch concluído. {} sucesso(s), {} falha(s).", ok, fail);
    return ok;
  }

  /** Serializa para JSON sem propagar JsonProcessingException (apenas para LOG). */
  private String safeJson(Object obj) {
    try {
      return om.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      return "<json-error:" + e.getOriginalMessage() + ">";
    }
  }
}
