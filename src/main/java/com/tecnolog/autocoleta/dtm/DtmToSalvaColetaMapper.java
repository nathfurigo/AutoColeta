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
    
    /**
     * Aplica a conversão de Metros para Centímetros nos volumes.
     * Esta função corrige os dados que vieram em metros da view (json_pedidocoleta)
     * e prepara-os para a API C# que espera o valor em CM.
     */
    private void corrigirEscalaDimensoes(List<SalvaColetaDimensoesModel> dimensoes, long idDtm) {
        if (dimensoes == null) {
            return;
        }

        for (SalvaColetaDimensoesModel dim : dimensoes) {
            
            Double compMetros = dim.getComp();
            Double largMetros = dim.getLarg();
            Double altMetros = dim.getAlt();

            if (compMetros != null && compMetros < 10.0) { // Assume que valores < 10.0 são em metros. Ex: 0.3
                dim.setComp(multiplicarPorCemEArredondar(compMetros));
                log.debug("DTM {}: Comp corrigido de {}m para {}cm.", idDtm, compMetros, dim.getComp());
            }
            if (largMetros != null && largMetros < 10.0) {
                dim.setLarg(multiplicarPorCemEArredondar(largMetros));
                log.debug("DTM {}: Larg corrigido de {}m para {}cm.", idDtm, largMetros, dim.getLarg());
            }
            if (altMetros != null && altMetros < 10.0) {
                dim.setAlt(multiplicarPorCemEArredondar(altMetros));
                log.debug("DTM {}: Alt corrigido de {}m para {}cm.", idDtm, altMetros, dim.getAlt());
            }
        }
    }
    
    /**
     * Converte metros para centímetros (x 100) e arredonda para duas casas.
     */
    private Double multiplicarPorCemEArredondar(Double meters) {
        // Usa BigDecimal para precisão na multiplicação e arredondamento
        return BigDecimal.valueOf(meters)
                         .multiply(BigDecimal.valueOf(100.0))
                         .setScale(2, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    public SalvaColetaModel map(DtmPendingRow row) {
        try {
            SalvaColetaModel req = om.readValue(row.getJsonPedidoColeta(), SalvaColetaModel.class);

            req.setIdDtm(row.getIdDtm());
            
            // Aplica a correção de escala de Metros para Centímetros no payload desserializado
            corrigirEscalaDimensoes(req.getDimensoes(), req.getIdDtm());

            log.debug("Mapeamento para DTM {}: idDtm foi definido como {} no payload.", row.getIdDtm(), req.getIdDtm());

            return req;

        } catch (Exception e) {
            throw new IllegalStateException("Falha ao mapear JSON da view para SalvaColetaModel para DTM " + row.getIdDtm(), e);
        }
    }
}