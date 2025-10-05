// DtmToSalvarColetaMapper.java
package com.tecnolog.autocoleta.dtm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
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

    public SalvaColetaModel map(DtmPendingRow row) {
        try {
            SalvaColetaModel model =
                om.readValue(row.getJsonPedidoColeta(), SalvaColetaModel.class);

            if (model.getTokenHash() == null || model.getTokenHash().isBlank()) {
                model.setTokenHash(defaultTokenHash);
            }
            return model;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Falha ao converter json_pedidocoleta da DTM " + row.getIdDtm(), e);
        }
    }
}
