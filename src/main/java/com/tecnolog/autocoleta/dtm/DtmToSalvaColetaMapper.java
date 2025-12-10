package com.tecnolog.autocoleta.dtm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaDimensoesModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaNFModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.tecnolog.autocoleta.service.FeriadoCacheService;

@Component
public class DtmToSalvaColetaMapper {

    private static final Logger log = LoggerFactory.getLogger(DtmToSalvaColetaMapper.class);
    
    private final ObjectMapper om;
    
    private final FeriadoCacheService feriadoService;

    public DtmToSalvaColetaMapper(ObjectMapper om, FeriadoCacheService feriadoService) {
        this.om = om;
        this.feriadoService = feriadoService; 
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
            SalvaColetaModel model = om.readValue(row.getJsonPedidoColeta(), SalvaColetaModel.class);
            
            model.setIdDtm(idDtm);

            if (model.getDimensoes() != null && !model.getDimensoes().isEmpty()) {
                for (SalvaColetaDimensoesModel dim : model.getDimensoes()) {
                    Double compM = dim.getComp();
                    Double largM = dim.getLarg();
                    Double altM = dim.getAlt();

                    dim.setComp(metrosParaCentimetros(compM));
                    dim.setLarg(metrosParaCentimetros(largM));
                    dim.setAlt(metrosParaCentimetros(altM));
                }
            } else {
                 log.warn("DTM {}: Nenhuma dimensão encontrada no JSON.", idDtm);
            }

            if ((model.getNf() == null || model.getNf().isEmpty()) && 
                 model.getVlTotalNF() != null && 
                 model.getVlTotalNF().compareTo(BigDecimal.ZERO) > 0) {
                
                SalvaColetaNFModel nfGenerica = new SalvaColetaNFModel();
                nfGenerica.setNr("0");
                nfGenerica.setVl(model.getVlTotalNF()); 
                List<SalvaColetaNFModel> nfs = new ArrayList<>();
                nfs.add(nfGenerica);
                model.setNf(nfs);
            }

            LocalDate dtColetaOriginal = model.getDtColeta(); 
            
            if (dtColetaOriginal == null) {
                log.warn("DTM {}: dtColeta está nula no JSON.", idDtm);
            } else {

                if (model.isAgendamentoFixo()) {

                    try {
                        LocalDate dtValidada = feriadoService.getProximoDiaUtil(dtColetaOriginal);
                        
                        if (!dtColetaOriginal.isEqual(dtValidada)) {
                             log.warn("DTM {}: Data de Agendamento Fixo ({}) caiu em feriado/fds. Movida para o dia útil seguinte: {}.", 
                                      idDtm, dtColetaOriginal, dtValidada);
                             model.setDtColeta(dtValidada);
                        } else {
                             log.info("DTM {}: Data de Agendamento Fixo ({}) vinda do Banco mantida.", idDtm, dtColetaOriginal);
                        }
                    } catch (Exception e) {
                        log.error("DTM {}: Erro ao validar data fixa.", idDtm, e);
                    }

                } else {
                    try {
                        LocalDate hoje = LocalDate.now();
                        LocalDate dtBase = dtColetaOriginal.isBefore(hoje) ? hoje : dtColetaOriginal;
                        LocalDate dtColetaFinal = feriadoService.getProximoDiaUtil(dtBase);

                        if (!dtColetaOriginal.isEqual(dtColetaFinal)) {
                            log.warn("DTM {}: Reagendamento JAVA (Normal). Data ajustada de '{}' para '{}'.",
                                     idDtm, dtColetaOriginal, dtColetaFinal);
                            model.setDtColeta(dtColetaFinal);
                        }
                    } catch (Exception e) {
                         log.error("DTM {}: Erro ao validar data normal.", idDtm, e);
                    }
                }
            }
            
            model.setDsAgenteNome(null);
            model.setDsAgenteEmail(null);

            return model;

        } catch (Exception e) {
            log.error("Falha ao mapear JSON DTM {}: {}", idDtm, e.getMessage());
            throw new IllegalStateException("Erro de Mapeamento DTM " + idDtm, e);
        }
    }
}