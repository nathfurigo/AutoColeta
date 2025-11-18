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
                nfGenerica.setVl(model.getVlTotalNF()); 
                List<SalvaColetaNFModel> nfs = new ArrayList<>();
                nfs.add(nfGenerica);
                model.setNf(nfs);
            }
            LocalDate dtColetaOriginal = model.getDtColeta(); 
            
            if (dtColetaOriginal == null) {
                log.warn("DTM {}: dtColeta está nula no JSON, não é possível reagendar.", idDtm);
            
            } else {

                if (!model.isAgendamentoFixo()) {
                    try {
                        LocalDate hoje = LocalDate.now();
                        LocalDate dtBase;

                        if (dtColetaOriginal.isBefore(hoje)) {
                            dtBase = hoje;
                            log.warn("DTM {}: Data original ({}) estava no passado. Usando HOJE ({}) como base para reagendamento.", 
                                     idDtm, dtColetaOriginal, hoje);
                        } else {
                            dtBase = dtColetaOriginal; 
                        }

                        LocalDate dtColetaFinal = feriadoService.getProximoDiaUtil(dtBase);

                        if (!dtColetaOriginal.isEqual(dtColetaFinal)) {
                            log.warn("DTM {}: Reagendamento JAVA (Normal/Emergência). Data base '{}' ajustada para próximo dia útil: '{}'.",
                                     idDtm, dtBase, dtColetaFinal);
                            
                            model.setDtColeta(dtColetaFinal);
                        } else {
                            log.debug("DTM {}: Data da coleta {} (Normal/Emergência) é um dia útil válido.", idDtm, dtColetaFinal);
                        }
                    } catch (Exception e) {
                        log.error("DTM {}: Falha ao validar/reagendar data da coleta (Normal/Emergência). Erro: {}", idDtm, e.getMessage());
                    }
                
                } else {
                    try {
                        LocalDate dtColetaAgendada = dtColetaOriginal;
                        
                        LocalDate dtVerificada = feriadoService.getProximoDiaUtil(dtColetaAgendada);

                        while (!dtVerificada.isEqual(dtColetaAgendada)) { 
                            log.warn("DTM {}: Data do Agendamento Fixo ({}) caiu em FDS/Feriado. Verificando a próxima semana (adicionando 7 dias).", 
                                     idDtm, dtColetaAgendada);
                            
                            dtColetaAgendada = dtColetaAgendada.plusDays(7);
                            dtVerificada = feriadoService.getProximoDiaUtil(dtColetaAgendada);
                        }
                        
                        if (!dtColetaOriginal.isEqual(dtColetaAgendada)) {
                            log.warn("DTM {}: Data do Agendamento Fixo reajustada de '{}' para '{}' (próxima data válida no mesmo dia da semana).", 
                                     idDtm, dtColetaOriginal, dtColetaAgendada);
                            model.setDtColeta(dtColetaAgendada);
                        } else {
                            log.debug("DTM {}: Data do Agendamento Fixo {} é um dia útil válido e foi mantida.", idDtm, dtColetaAgendada);
                        }
                    } catch (Exception e) {
                        log.error("DTM {}: Falha ao validar/reagendar data da coleta (Agendamento Fixo). Erro: {}", idDtm, e.getMessage());
                    }
                }
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