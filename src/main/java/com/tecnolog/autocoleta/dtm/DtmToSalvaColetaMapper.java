package com.tecnolog.autocoleta.dtm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DtmToSalvaColetaMapper {

    private static final Logger log = LoggerFactory.getLogger(DtmToSalvaColetaMapper.class);

    private final ObjectMapper om;

    public DtmToSalvaColetaMapper(ObjectMapper om) {
        this.om = om;
    }

    public SalvaColetaModel map(DtmPendingRow row) {
        try {
            SalvaColetaModel req = om.readValue(row.getJsonPedidoColeta(), SalvaColetaModel.class);

            req.setIdDtm(row.getIdDtm());

            log.debug("Mapeamento para DTM {}: idDtm foi definido como {} no payload.", row.getIdDtm(), req.getIdDtm());

            return req;

        } catch (Exception e) {
            throw new IllegalStateException("Falha ao mapear JSON da view para SalvaColetaModel para DTM " + row.getIdDtm(), e);
        }
    }
}