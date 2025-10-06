package com.tecnolog.autocoleta.dtm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.config.AppProperties;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaDimensoesModel;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaNFModel;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaMonitoramentoModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.tecnolog.autocoleta.dto.SalvaColetaModel;

@Component
public class DtmToSalvaColetaMapper {

    private static final Logger log = LoggerFactory.getLogger(DtmToSalvaColetaMapper.class);

    private final ObjectMapper om;
    private final AppProperties props;
    private final DateTimeFormatter dtmDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    public DtmToSalvaColetaMapper(ObjectMapper om, AppProperties props) {
        this.om = om;
        this.props = props;
    }

    public SalvaColetaModel map(DtmPendingRow row) {
        try {
            DtmJson dtmJson = om.readValue(row.getJsonPedidoColeta(), DtmJson.class);
            SalvaColetaModel req = new SalvaColetaModel();
            AppProperties.Defaults defaults = props.getDefaults();

            // --- MAPEAMENTO ALINHADO AO JSON ---
            req.setTokenHash(props.getSalvarColeta().getTokenHash());
            req.setIdPedidoColeta(0);

            // IDs padrão
            req.setIdRemetente(defaults.getIdRemetente());
            req.setIdDestinatario(defaults.getIdDestinatario());
            req.setIdTomador(defaults.getIdTomador());
            req.setIdLocalColeta(defaults.getIdLocalColeta());
            req.setIdFilialResposavel(defaults.getIdFilialResposavel()); // <-- com 's'
            req.setIdTipoColeta(defaults.getIdTipoColetaDefault());
            req.setIdAgente(defaults.getIdAgente());

            // Datas e horas
            if (dtmJson.getDtInicio() != null && !dtmJson.getDtInicio().isBlank()) {
                req.setDtColeta(LocalDate.parse(dtmJson.getDtInicio(), dtmDateFormatter));
            }
            if (dtmJson.getDtFim() != null && !dtmJson.getDtFim().isBlank()) {
                req.setDtEntrega(LocalDate.parse(dtmJson.getDtFim(), dtmDateFormatter));
            }
            req.setHrColetaInicio(defaults.getHrInicio()); // "08:00"
            req.setHrColetaFim(defaults.getHrFim());       // "17:00"

            // Modal
            String modal = dtmJson.getTipoServico() != null ? dtmJson.getTipoServico().toLowerCase() : "";
            req.setTpModal(modal.contains("aereo") ? 0 : 1);

            // Endereço (nomes iguais ao JSON)
            DtmJson.Endpoint origem = dtmJson.getOrigem();
            if (origem != null) {
                req.setCdEnderecoCEP(origem.getCep());
                req.setDsEndereco(origem.getEndereco());
                req.setNrEnderecoNR(origem.getNumero());
                req.setDsEnderecoBairro(origem.getBairro());
                req.setDsEnderecoComplento(origem.getComplemento()); // mantém o "Complento"
                req.setIdEnderecoCidade(defaults.getIdEnderecoCidade() != 999 ? defaults.getIdEnderecoCidade() : null);
            }

            // Contatos / solicitante
            String solicitante = dtmJson.getSolicitante();
            req.setDsSolicitante(solicitante != null && !solicitante.isBlank() ? solicitante : "TESTE");

            if (origem != null && origem.getContatosOrigem() != null && !origem.getContatosOrigem().isEmpty()) {
                DtmJson.Contato c = origem.getContatosOrigem().get(0);
                req.setDsProcurarPor(c.getNome() != null && !c.getNome().isBlank() ? c.getNome() : "TESTE");
                req.setNrTelefone(c.getTelefone() != null && !c.getTelefone().isBlank() ? c.getTelefone() : "00000000000");
            } else {
                req.setDsProcurarPor("TESTE");
                req.setNrTelefone("00000000000");
            }

            // Referências
            req.setNrReferencia(dtmJson.getReferencia());
            req.setNrPedidoCliente(String.valueOf(dtmJson.getDtm()));

            // Carga (ids numéricos opcionais)
            if (dtmJson.getCargas() != null && !dtmJson.getCargas().isEmpty()) {
                DtmJson.Carga c = dtmJson.getCargas().get(0);
                req.setIdEmbalagem(getIntegerOrNull(c.getEmbalagem()));
                req.setIdNaturezaCarga(getIntegerOrNull(c.getDescricao()));
            } else {
                req.setIdEmbalagem(1);
                req.setIdNaturezaCarga(1);
            }

            // Comentários
            req.setDsComentarios(dtmJson.getObservacoes());

            // NF (id, nr, vl, chave)
            if (dtmJson.getNotasFiscaisRelacionadas() != null) {
                List<SalvaColetaNFModel> nfList = dtmJson.getNotasFiscaisRelacionadas().stream().map(nota -> {
                    SalvaColetaNFModel nf = new SalvaColetaNFModel();
                    nf.setId(0);
                    nf.setChave(" "); // igual ao exemplo
                    
                    // CORREÇÃO APLICADA AQUI
                    String serieSuf = (nota.getSerie() != null) ? "-" + nota.getSerie() : "";

                    nf.setNr(nota.getNumero() + serieSuf);
                    nf.setVl(nota.getValor() != null ? new BigDecimal(String.format(Locale.US, "%.2f", nota.getValor())) : BigDecimal.ZERO);
                    return nf;
                }).collect(Collectors.toList());
                req.setNf(nfList); 
            }

            if (dtmJson.getCargas() != null) {
                List<SalvaColetaDimensoesModel> dims = dtmJson.getCargas().stream().map(carga -> {
                    SalvaColetaDimensoesModel d = new SalvaColetaDimensoesModel();
                    d.setId(0);
                    d.setComp(carga.getComp());
                    d.setLarg(carga.getLarg());
                    d.setAlt(carga.getAlt());
                    d.setQt(carga.getQuantidade());
                    d.setKg(carga.getPesoBruto());
                    return d;
                }).collect(Collectors.toList());
                req.setDimensoes(dims); // "Dimensoes" com D maiúsculo
            }

            // Monitoramento (nome, telefone, email)
            if (dtmJson.getDestino() != null && dtmJson.getDestino().getContatosDestino() != null) {
                List<SalvaColetaMonitoramentoModel> mons =
                        dtmJson.getDestino().getContatosDestino().stream().map(ct -> {
                            SalvaColetaMonitoramentoModel m = new SalvaColetaMonitoramentoModel();
                            m.setNome(ct.getNome());
                            m.setTelefone(ct.getTelefone());
                            m.setEmail(ct.getEmail());
                            return m;
                        }).collect(Collectors.toList());
                req.setMonitoramento(mons); // "Monitoramento" com M maiúsculo
            }

            return req;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao mapear DTM " + row.getIdDtm() + " para SalvaColetaModel", e);
        }
    }

    private Integer getIntegerOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) {
            log.warn("Valor '{}' não pôde ser convertido para número. Retornando nulo.", value);
            return null;
        }
    }
}