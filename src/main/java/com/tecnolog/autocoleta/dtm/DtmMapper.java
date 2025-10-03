package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import com.tecnolog.autocoleta.config.AppProperties.Defaults;
import com.tecnolog.autocoleta.domain.Modal;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaDimensoesModel;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

@Component
public class DtmMapper {
    private final AppProperties props;
    public DtmMapper(AppProperties props) { this.props = props; }

    public SalvaColetaModel toSalvarColeta(DtmPendingRow r) {
        Defaults def = props.getDefaults();
        SalvaColetaModel m = new SalvaColetaModel();

        m.idRemetente = def.getIdRemetente();
        m.idDestinatario = def.getIdDestinatario();
        m.idTomador = def.getIdTomador();
        m.idLocalColeta = def.getIdLocalColeta();

        // Datas/horários
        LocalDate hoje = LocalDate.now();
        m.dtColeta  = (r.getDtColetaPrev()  != null ? r.getDtColetaPrev().toLocalDate()  : hoje);
        m.dtEntrega = (r.getDtEntregaPrev() != null ? r.getDtEntregaPrev().toLocalDate() : hoje.plusDays(1));
        m.hrColetaInicio = def.getHrInicio();
        m.hrColetaFim    = def.getHrFim();

        // Tipo/Modal
        String ns = r.getNivelServico() == null ? "" : r.getNivelServico().toUpperCase();
        m.idTipoColeta = ns.contains("EMER") ? 1 : (ns.contains("EXPRESS") ? 2 : def.getIdTipoColetaDefault());

        Modal modal = def.getModal();
        m.tpModal = (modal == Modal.AEREO ? 0 : 1);

        m.idFilialResposavel = def.getIdFilialResposavel();
        m.idAgente = null;

        // Endereço da coleta (origem)
        m.cdEnderecoCEP       = nz(r.getOrigemCep(), "00000-000");
        m.dsEndereco          = nz(r.getOrigemEndereco(), "ENDERECO NAO INFORMADO");
        m.nrEnderecoNR        = nz(r.getOrigemNumero(), "S/N");
        m.dsEnderecoBairro    = nz(r.getOrigemBairro(), "");
        m.dsEnderecoComplento = nz(r.getOrigemCompl(), "");
        m.idEnderecoCidade    = def.getIdEnderecoCidade();

        // Contatos
        m.nrTelefone    = "";
        m.dsSolicitante = nz(r.getOrigemNome(), "Automação");
        m.dsProcurarPor = m.dsSolicitante;

        // Dimensões/NFs mínimas (mock)
        SalvaColetaDimensoesModel d = new SalvaColetaDimensoesModel();
        d.id = 0; d.qt = 1; d.comp = 30; d.larg = 30; d.alt = 30; d.kg = 1;

        m.Dimensoes = Arrays.asList(d);
        m.NF = Collections.emptyList();
        m.Monitoramento = Collections.emptyList();

        m.dsComentarios = "Gerado automaticamente pela AutoColeta (DTM " + r.getIdDtm() + ")";
        return m;
    }

    private static String nz(String s, String def) {
        return (s == null || s.trim().isEmpty()) ? def : s;
    }
}
