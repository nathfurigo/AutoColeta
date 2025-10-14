package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaDimensoesModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaNFModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DtmJsonToSalvaColetaMapper {

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public SalvaColetaModel map(DtmJson dtmJson, long idDtm) {
        SalvaColetaModel model = new SalvaColetaModel();

        // 1. Mapeamento de IDs e Referências
        model.setIdDtm(idDtm);
        model.setNrReferencia(dtmJson.getDtm()); // O 'Id' do JSON parece ser a referência principal
        model.setNrPedidoCliente(dtmJson.getReferencia()); // 'Referencia' do JSON vira o pedido do cliente

        // 2. Mapeamento de Remetente, Destinatário e Tomador
        // A lógica de produção indica que o Remetente é também o Tomador (pagador)
        if (dtmJson.getOrigem() != null) {
            DtmJson.Endpoint origem = dtmJson.getOrigem();
            model.setDsRemetente(origem.getNome());
            model.setDsTomador(origem.getNome()); // Tomador igual ao Remetente
            model.setDsEndereco(origem.getEndereco());
            model.setNrEnderecoNR(origem.getNumero());
            model.setDsEnderecoBairro(origem.getBairro());
            model.setDsEnderecoComplento(origem.getComplemento());
            model.setCdEnderecoCEP(origem.getCep());

            if (origem.getContatosOrigem() != null && !origem.getContatosOrigem().isEmpty()) {
                DtmJson.Contato contatoOrigem = origem.getContatosOrigem().get(0);
                model.setDsProcurarPor(contatoOrigem.getNome());
                model.setNrTelefone(contatoOrigem.getTelefone());
            }
        }
        if (dtmJson.getDestino() != null) {
            model.setDsDestinatario(dtmJson.getDestino().getNome());
        }

        // 3. Mapeamento de Datas e Horas
        if (dtmJson.getDtInicio() != null && !dtmJson.getDtInicio().isBlank()) {
            LocalDateTime dtInicio = LocalDateTime.parse(dtmJson.getDtInicio(), ISO_DATE_TIME_FORMATTER);
            model.setDtColeta(dtInicio.toLocalDate());
            model.setHrColetaInicio(dtInicio.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (dtmJson.getDtFim() != null && !dtmJson.getDtFim().isBlank()) {
            LocalDateTime dtFim = LocalDateTime.parse(dtmJson.getDtFim(), ISO_DATE_TIME_FORMATTER);
            model.setDtEntrega(dtFim.toLocalDate());
        }

        // 4. Mapeamento do Modal (Aéreo/Rodoviário) - Regra crucial para bater com Produção
        // Em produção, o modal parece ser Aéreo ('A'). Vamos definir essa regra.
        // tpModal: 1 = Rodo, 2 = Aéreo
        String nivelServico = dtmJson.getNivelServico() != null ? dtmJson.getNivelServico().toUpperCase() : "";
        if (nivelServico.contains("AEREO") || nivelServico.contains("AÉREO")) {
            model.setTpModal(2); // Aéreo
        } else {
            model.setTpModal(1); // Rodoviário (padrão)
        }

        // 5. Mapeamento de Agente e Solicitante
        model.setDsAgenteNome(dtmJson.getAnalista());
        model.setDsSolicitanteNome(dtmJson.getSolicitante());

        // 6. Mapeamento de Notas Fiscais (Estruturado)
        if (dtmJson.getNotasFiscaisRelacionadas() != null) {
            List<SalvaColetaNFModel> nfs = dtmJson.getNotasFiscaisRelacionadas().stream()
                .map(nota -> {
                    SalvaColetaNFModel nfModel = new SalvaColetaNFModel();
                    nfModel.setNr(nota.getNumero() != null ? nota.getNumero().toString() : null);
                    nfModel.setVl(nota.getValor() != null ? BigDecimal.valueOf(nota.getValor()) : null);
                    // A chave não está no DtmJson, será preenchida posteriormente se necessário
                    return nfModel;
                }).collect(Collectors.toList());
            model.setNf(nfs);
        }

        // 7. Mapeamento das Dimensões/Cargas e definição da Embalagem/Natureza principal
        if (dtmJson.getCargas() != null && !dtmJson.getCargas().isEmpty()) {
            List<SalvaColetaDimensoesModel> dimensoes = dtmJson.getCargas().stream()
                .map(carga -> {
                    SalvaColetaDimensoesModel dim = new SalvaColetaDimensoesModel();
                    dim.setComp(carga.getComp());
                    dim.setLarg(carga.getLarg());
                    dim.setAlt(carga.getAlt());
                    dim.setQt(carga.getQuantidade());
                    dim.setKg(carga.getPesoBruto());
                    return dim;
                }).collect(Collectors.toList());
            model.setDimensoes(dimensoes);

            // Usa a primeira carga para definir a embalagem e natureza principal da coleta
            DtmJson.Carga primeiraCarga = dtmJson.getCargas().get(0);
            model.setDsEmbalagem(primeiraCarga.getEmbalagem());
            model.setDsNaturezaCarga(primeiraCarga.getDescricao());
        }
        
        // 8. Observações
        model.setDsComentarios(dtmJson.getObservacoes());

        return model;
    }
}