package com.tecnolog.autocoleta.dtm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaDimensoesModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DtmToSalvaColetaMapper {

    private static final Logger log = LoggerFactory.getLogger(DtmToSalvaColetaMapper.class);

    private final ObjectMapper om;

    public DtmToSalvaColetaMapper(ObjectMapper om) {
        this.om = om;
    }

    private void corrigirEscalaDimensoes(List<SalvaColetaDimensoesModel> dimensoes, long idDtm) {
        if (dimensoes == null) {
            log.warn("DTM {}: Lista de dimensões nula, não foi possível corrigir escala.", idDtm);
            return;
        }

        final double LIMITE_METROS = 10.0;

        for (SalvaColetaDimensoesModel dim : dimensoes) {
            Double compOriginal = dim.getComp();
            Double largOriginal = dim.getLarg();
            Double altOriginal = dim.getAlt();
            boolean corrigido = false;

            if (compOriginal != null && compOriginal > 0 && compOriginal < LIMITE_METROS) {
                dim.setComp(multiplicarPorCemEArredondar(compOriginal));
                log.debug("DTM {}: Comp corrigido de {}m para {}cm.", idDtm, compOriginal, dim.getComp());
                corrigido = true;
            }
            if (largOriginal != null && largOriginal > 0 && largOriginal < LIMITE_METROS) {
                dim.setLarg(multiplicarPorCemEArredondar(largOriginal));
                log.debug("DTM {}: Larg corrigido de {}m para {}cm.", idDtm, largOriginal, dim.getLarg());
                corrigido = true;
            }
            if (altOriginal != null && altOriginal > 0 && altOriginal < LIMITE_METROS) {
                dim.setAlt(multiplicarPorCemEArredondar(altOriginal));
                log.debug("DTM {}: Alt corrigido de {}m para {}cm.", idDtm, altOriginal, dim.getAlt());
                corrigido = true;
            }

        }
    }

    private Double multiplicarPorCemEArredondar(Double meters) {
        if (meters == null) return null;
        return BigDecimal.valueOf(meters)
                         .multiply(BigDecimal.valueOf(100.0))
                         .setScale(2, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    public SalvaColetaModel map(DtmPendingRow row) {
        if (row == null || row.getJsonPedidoColeta() == null || row.getJsonPedidoColeta().isBlank()) {
             log.error("Tentativa de mapear DtmPendingRow nula ou com JSON vazio.");
             throw new IllegalArgumentException("DtmPendingRow inválido para mapeamento.");
        }

        try {
            SalvaColetaModel req = om.readValue(row.getJsonPedidoColeta(), SalvaColetaModel.class);

            req.setIdDtm(row.getIdDtm());

            corrigirEscalaDimensoes(req.getDimensoes(), req.getIdDtm());

            log.debug("Mapeamento do JSON da view para DTM {} concluído. ID {} definido.", row.getIdDtm(), req.getIdDtm());

            return req;

        } catch (Exception e) {
            log.error("Falha ao mapear JSON da view para SalvaColetaModel para DTM {}. Causa: {}. JSON: {}",
                      row.getIdDtm(), e.getMessage(), row.getJsonPedidoColeta(), e);
            throw new IllegalStateException("Falha ao mapear JSON da view para SalvaColetaModel para DTM " + row.getIdDtm() + ". Verifique o formato do JSON ou o mapeamento da classe.", e);
        }
    }
}