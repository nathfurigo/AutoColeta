package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaDimensoesModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaNFModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException; 
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DtmJsonToSalvaColetaMapper {

    private static final Logger log = LoggerFactory.getLogger(DtmJsonToSalvaColetaMapper.class);
    private static final DateTimeFormatter DTM_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter API_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private Double metrosParaCentimetros(Double metros) {
        if (metros == null) {
            return null;
        }
        return BigDecimal.valueOf(metros)
                         .multiply(BigDecimal.valueOf(100.0))
                         .setScale(2, RoundingMode.HALF_UP)
                         .doubleValue();
    }


    public SalvaColetaModel map(DtmJson dtmJson, long idDtm) {
        SalvaColetaModel model = new SalvaColetaModel();
        model.setIdDtm(idDtm);
        model.setNrReferencia(dtmJson.getReferencia() != null ? dtmJson.getReferencia() : String.valueOf(idDtm)); 
        model.setNrPedidoCliente(String.valueOf(idDtm)); 
        model.setDsTomador(null);
        model.setCdTomadorCnpj("33000167000101");

        if (dtmJson.getOrigem() != null) {
            DtmJson.Endpoint origem = dtmJson.getOrigem();
            model.setDsRemetente(origem.getNome()); 
            model.setDsEndereco(origem.getEndereco());
            model.setNrEnderecoNR(origem.getNumero());
            model.setDsEnderecoBairro(origem.getBairro());
            model.setDsEnderecoComplento(origem.getComplemento());
            model.setCdEnderecoCEP(origem.getCep());
            model.setDsCidadeColeta(origem.getCidade());
            model.setDsCidadeColetaUF(origem.getUF());

            if (origem.getContatosOrigem() != null && !origem.getContatosOrigem().isEmpty()) {
                DtmJson.Contato contatoOrigem = origem.getContatosOrigem().get(0);
                model.setDsProcurarPor(contatoOrigem.getNome());
                model.setNrTelefone(contatoOrigem.getTelefone());
            } else {
                 log.warn("DTM {}: Nenhum contato de origem encontrado no JSON.", idDtm);
            }
        } else {
            log.error("DTM {}: Dados de Origem (Remetente/Local Coleta) ausentes no JSON.", idDtm);
        }

        if (dtmJson.getDestino() != null) {
            DtmJson.Endpoint destino = dtmJson.getDestino();
            model.setDsDestinatario(destino.getNome());
            model.setDsCidadeDestino(destino.getCidade());
        } else {
             log.error("DTM {}: Dados de Destino ausentes no JSON.", idDtm);
        }

        try {
            if (dtmJson.getDtInicio() != null && !dtmJson.getDtInicio().isBlank()) {
                LocalDateTime dtInicio = LocalDateTime.parse(dtmJson.getDtInicio(), DTM_DATE_TIME_FORMATTER);
                model.setDtColeta(dtInicio.toLocalDate());
                model.setHrColetaInicio(dtInicio.toLocalTime().format(API_TIME_FORMATTER));
            } else {
                 log.warn("DTM {}: DtInicio ausente no JSON. DtColeta/HrColetaInicio não serão definidos.", idDtm);
            }
            if (dtmJson.getDtFim() != null && !dtmJson.getDtFim().isBlank()) {
                LocalDateTime dtFim = LocalDateTime.parse(dtmJson.getDtFim(), DTM_DATE_TIME_FORMATTER);
                model.setDtEntrega(dtFim.toLocalDate());
            } else {
                 log.warn("DTM {}: DtFim ausente no JSON. DtEntrega não será definida.", idDtm);
            }
        } catch (DateTimeParseException e) {
             log.error("DTM {}: Erro ao parsear datas DtInicio ('{}') ou DtFim ('{}'). Formato esperado: {}. Erro: {}",
                       idDtm, dtmJson.getDtInicio(), dtmJson.getDtFim(), DTM_DATE_TIME_FORMATTER, e.getMessage());
             model.setDtColeta(null);
             model.setHrColetaInicio(null);
             model.setDtEntrega(null);
        }

        // --- INÍCIO DA CORREÇÃO ---
        // O Analista do JSON não é o Agente da coleta.
        // Deixamos nulo para que o SqlServerRepository use o Agente padrão.
        model.setDsAgenteNome(null); 
        model.setDsAgenteEmail(null);
        // --- FIM DA CORREÇÃO ---
        
        model.setDsSolicitante(dtmJson.getSolicitante()); 
        model.setDsSolicitanteNome(null);

        if (dtmJson.getNotasFiscaisRelacionadas() != null) {
            List<SalvaColetaNFModel> nfs = dtmJson.getNotasFiscaisRelacionadas().stream()
                .map(nota -> {
                    SalvaColetaNFModel nfModel = new SalvaColetaNFModel();
                    String nrFormatado = nota.getNumero() != null ? nota.getNumero().toString() : "";
                    if (nota.getSerie() != null && nota.getSerie() > 0) { 
                        nrFormatado += "-" + nota.getSerie().toString();
                    }
                    nfModel.setNr(nrFormatado);
                    nfModel.setVl(nota.getValor() != null ? BigDecimal.valueOf(nota.getValor()) : BigDecimal.ZERO); 
                    return nfModel;
                }).collect(Collectors.toList());
            model.setNf(nfs);
            model.setVlTotalNF(nfs.stream()
                                  .map(SalvaColetaNFModel::getVl)
                                  .reduce(BigDecimal.ZERO, BigDecimal::add));
        } else {
             model.setNf(new ArrayList<>());
             model.setVlTotalNF(BigDecimal.ZERO);
        }

        if (dtmJson.getCargas() != null && !dtmJson.getCargas().isEmpty()) {
            List<SalvaColetaDimensoesModel> dimensoes = dtmJson.getCargas().stream()
                .map(carga -> {
                    SalvaColetaDimensoesModel dim = new SalvaColetaDimensoesModel();

                    Double compCM = metrosParaCentimetros(carga.getComp());
                    Double largCM = metrosParaCentimetros(carga.getLarg());
                    Double altCM = metrosParaCentimetros(carga.getAlt());

                    dim.setComp(compCM);
                    dim.setLarg(largCM);
                    dim.setAlt(altCM);
                    dim.setQt(carga.getQuantidade() != null ? carga.getQuantidade() : 0); 
                    dim.setKg(carga.getPesoBruto() != null ? carga.getPesoBruto() : 0.0); 

                    log.debug("DTM {}: Dimensões mapeadas - Entrada(M): C:{}/L:{}/A:{} -> Saída(CM): C:{}/L:{}/A:{} Qtd:{} Peso:{}",
                              idDtm,
                              carga.getComp(), carga.getLarg(), carga.getAlt(),
                              compCM, largCM, altCM, dim.getQt(), dim.getKg());

                    return dim;
                }).collect(Collectors.toList());
            model.setDimensoes(dimensoes);

            DtmJson.Carga primeiraCarga = dtmJson.getCargas().get(0);
            model.setDsEmbalagem(primeiraCarga.getEmbalagem());
            model.setDsNaturezaCarga(primeiraCarga.getDescricao());
        } else {
             log.error("DTM {}: Nenhuma Carga/Dimensão encontrada no JSON. Embalagem e Natureza não serão definidas.", idDtm);
             model.setDimensoes(new ArrayList<>());
        }

         model.setTpModal(null);
         model.setDsTipoColeta(dtmJson.getNivelServico());
        model.setDsComentarios(dtmJson.getObservacoes());

        List<com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaMonitoramentoModel> monitoramentoList = new ArrayList<>();
        if (model.getDsProcurarPor() != null || model.getNrTelefone() != null) {
            com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaMonitoramentoModel monitor = new com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaMonitoramentoModel();
            monitor.setNome(model.getDsProcurarPor());
            monitor.setTelefone(model.getNrTelefone());
            monitoramentoList.add(monitor);
        }
        model.setMonitoramento(monitoramentoList);

        return model;
    }
}