package com.tecnolog.autocoleta.dtm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaDimensoesModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaNFModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class DtmToSalvaColetaMapper {

    private static final Logger log = LoggerFactory.getLogger(DtmToSalvaColetaMapper.class);
    private final ObjectMapper om;

    public DtmToSalvaColetaMapper(ObjectMapper om) {
        this.om = om;
    }
    private Double metrosParaCentimetros(Double metros) {
        if (metros == null) {
            return null;
        }
        return BigDecimal.valueOf(metros)
                         .multiply(BigDecimal.valueOf(100.0))
                         .setScale(2, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    public SalvaColetaModel map(DtmPendingRow row) {
        if (row == null || row.getJsonPedidoColeta() == null || row.getJsonPedidoColeta().isBlank()) {
             log.error("Tentativa de mapear DtmPendingRow nula ou com JSON vazio.");
             throw new IllegalArgumentException("DtmPendingRow inválido para mapeamento.");
        }

        long idDtm = row.getIdDtm();

        try {
            // 1. Desserializar DIRETAMENTE para o modelo final (SalvaColetaModel)
            SalvaColetaModel model = om.readValue(row.getJsonPedidoColeta(), SalvaColetaModel.class);
            
            // 2. Definir o ID da DTM (que não vem no JSON interno)
            model.setIdDtm(idDtm);

            // 3. CORRIGIR AS DIMENSÕES (M para CM)
            if (model.getDimensoes() != null && !model.getDimensoes().isEmpty()) {
                for (SalvaColetaDimensoesModel dim : model.getDimensoes()) {
                    
                    Double compM = dim.getComp(); // Valor original (ex: 0.15)
                    Double largM = dim.getLarg(); // Valor original (ex: 0.15)
                    Double altM = dim.getAlt();   // Valor original (ex: 0.15)

                    dim.setComp(metrosParaCentimetros(compM));
                    dim.setLarg(metrosParaCentimetros(largM));
                    dim.setAlt(metrosParaCentimetros(altM));
                    
                    log.debug("DTM {}: Dimensões corrigidas - Entrada(M): C:{}/L:{}/A:{} -> Saída(CM): C:{}/L:{}/A:{}",
                              idDtm, compM, largM, altM, dim.getComp(), dim.getLarg(), dim.getAlt());
                }
            } else {
                 log.warn("DTM {}: Nenhuma dimensão encontrada no JSON.", idDtm);
            }
            if ((model.getNf() == null || model.getNf().isEmpty()) && 
                 model.getVlTotalNF() != null && 
                 model.getVlTotalNF().compareTo(BigDecimal.ZERO) > 0) {
                
                log.warn("DTM {}: A lista NF estava vazia, mas vlTotalNF era {}. Criando uma NF genérica (Nr: 0) para compatibilidade com a API.",
                         model.getIdDtm(), model.getVlTotalNF());
                
                SalvaColetaNFModel nfGenerica = new SalvaColetaNFModel();
                
                nfGenerica.setNr("0");
                
                // Usa o valor total como o valor desta NF
                nfGenerica.setVl(model.getVlTotalNF()); 
                List<SalvaColetaNFModel> nfs = new ArrayList<>();
                nfs.add(nfGenerica);
                model.setNf(nfs);
            }

            model.setDsAgenteNome(null);
            model.setDsAgenteEmail(null);

            log.debug("Mapeamento direto do JSON para DTM {} concluído.", idDtm);
            return model;

        } catch (Exception e) {
            log.error("Falha ao mapear JSON da view para SalvaColetaModel para DTM {}. Causa: {}. JSON: {}",
                      idDtm, e.getMessage(), row.getJsonPedidoColeta(), e);
            throw new IllegalStateException("Falha ao mapear JSON da view para SalvaColetaModel para DTM " + idDtm + ". Verifique o formato do JSON.", e);
        }
    }
}