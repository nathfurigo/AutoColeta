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

/**
 * Componente responsável por converter (mapear) os dados brutos da tabela de DTM (DtmPendingRow)
 * para o modelo de negócio utilizado no salvamento da coleta (SalvaColetaModel).
 * Realiza tratamentos de JSON, conversão de unidades e validação de datas (dias úteis).
 */
@Component
public class DtmToSalvaColetaMapper {

    private static final Logger log = LoggerFactory.getLogger(DtmToSalvaColetaMapper.class);
    
    // Responsável pela serialização/deserialização do JSON
    private final ObjectMapper om;
    
    // Serviço para verificar feriados e fins de semana
    private final FeriadoCacheService feriadoService;

    public DtmToSalvaColetaMapper(ObjectMapper om, FeriadoCacheService feriadoService) {
        this.om = om;
        this.feriadoService = feriadoService; 
    }
    
    /**
     * Método utilitário para converter medidas de Metros para Centímetros.
     * Utiliza BigDecimal para garantir precisão e arredondamento correto (2 casas).
     */
    private Double metrosParaCentimetros(Double metros) {
        if (metros == null) {
            return null;
        }
        return BigDecimal.valueOf(metros)
                         .multiply(BigDecimal.valueOf(100.0))
                         .setScale(2, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    /**
     * Método principal de mapeamento.
     * @param row Objeto contendo o ID e o JSON bruto da DTM.
     * @return Modelo tratado pronto para ser processado pela API de salvar coleta.
     */
    public SalvaColetaModel map(DtmPendingRow row) {
        // 1. Validação inicial de integridade
        if (row == null || row.getJsonPedidoColeta() == null || row.getJsonPedidoColeta().isBlank()) {
             log.error("Tentativa de mapear DtmPendingRow nula ou com JSON vazio.");
             throw new IllegalArgumentException("DtmPendingRow inválido para mapeamento.");
        }

        long idDtm = row.getIdDtm();

        try {
            // 2. Deserialização: Converte a String JSON para o Objeto Java
            SalvaColetaModel model = om.readValue(row.getJsonPedidoColeta(), SalvaColetaModel.class);
            
            // Garante que o ID da DTM esteja vinculado ao modelo
            model.setIdDtm(idDtm);

            // 3. Tratamento de Dimensões (Conversão M -> CM)
            if (model.getDimensoes() != null && !model.getDimensoes().isEmpty()) {
                for (SalvaColetaDimensoesModel dim : model.getDimensoes()) {
                    Double compM = dim.getComp();
                    Double largM = dim.getLarg();
                    Double altM = dim.getAlt();

                    // Atualiza o objeto com os valores convertidos
                    dim.setComp(metrosParaCentimetros(compM));
                    dim.setLarg(metrosParaCentimetros(largM));
                    dim.setAlt(metrosParaCentimetros(altM));
                }
            } else {
                 log.warn("DTM {}: Nenhuma dimensão encontrada no JSON.", idDtm);
            }

            // 4. Tratamento de Nota Fiscal (NF)
            // Regra: Se não tem NF detalhada, mas tem Valor Total > 0, cria uma NF genérica "0"
            // isso evita erros de validação em coletas que exigem ao menos uma NF.
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

            // 5. Lógica de Datas e Agendamento
            LocalDate dtColetaOriginal = model.getDtColeta(); 
            
            if (dtColetaOriginal == null) {
                log.warn("DTM {}: dtColeta está nula no JSON.", idDtm);
            } else {

                if (model.isAgendamentoFixo()) {
                    // --- REGRA DE AGENDAMENTO FIXO ---
                    // Tenta manter a data original. Só altera se cair em Feriado/Fim de Semana.
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
                    // --- REGRA DE AGENDAMENTO NORMAL ---
                    // Garante que a data não seja no passado.
                    // Se a data original for anterior a hoje, assume 'Hoje' como base.
                    // Em seguida, busca o próximo dia útil.
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
            
            // 6. Limpeza de Dados do Agente
            // Remove informações do usuário logado para evitar inconsistências ou sobrescrita indevida
            model.setDsAgenteNome(null);
            model.setDsAgenteEmail(null);

            return model;

        } catch (Exception e) {
            log.error("Falha ao mapear JSON DTM {}: {}", idDtm, e.getMessage());
            throw new IllegalStateException("Erro de Mapeamento DTM " + idDtm, e);
        }
    }
}