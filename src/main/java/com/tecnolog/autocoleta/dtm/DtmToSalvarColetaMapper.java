package com.tecnolog.autocoleta.dtm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.dto.SalvarColetaRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DtmToSalvarColetaMapper {

  private final ObjectMapper om;
  private final String defaultTokenHash;

  public DtmToSalvarColetaMapper(ObjectMapper om,
                                 @Value("${app.salvarColeta.tokenHash}") String defaultTokenHash) {
    this.om = om;
    this.defaultTokenHash = defaultTokenHash;
  }

  public SalvarColetaRequest map(DtmPendingRow row) {
    try {
      SalvarColetaRequest req = om.readValue(row.getJsonPedidoColeta(), SalvarColetaRequest.class);
      if (req.getTokenHash() == null || req.getTokenHash().isBlank()) {
        req.setTokenHash(defaultTokenHash);
      }
      return req;
    } catch (Exception e) {
      throw new IllegalStateException("Falha ao converter json_pedidocoleta da DTM " + row.getIdDtm(), e);
    }
  }
}
