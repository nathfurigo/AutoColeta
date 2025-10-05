package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaDimensoesModel;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaModel;
import com.tecnolog.autocoleta.salvarcoleta.payload.SalvaColetaNFModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class DtmToSalvarColetaMapper {

    private final AppProperties props;

    public DtmToSalvarColetaMapper(AppProperties props) {
        this.props = props;
    }

    public SalvaColetaModel map(DtmPendingRow r) {
        SalvaColetaModel m = new SalvaColetaModel();

        // Id DTM (Long no DTO)
        m.setIdDtm(r.getIdDtm());

        // Defaults
        if (props.getDefaults() != null) {
            if (m.getIdRemetente() == null)        m.setIdRemetente(props.getDefaults().getIdRemetente());
            if (m.getIdDestinatario() == null)     m.setIdDestinatario(props.getDefaults().getIdDestinatario());
            if (m.getIdTomador() == null)          m.setIdTomador(props.getDefaults().getIdTomador());
            if (m.getIdLocalColeta() == null)      m.setIdLocalColeta(props.getDefaults().getIdLocalColeta());
            // ATENÇÃO: se o seu SalvaColetaModel tiver o campo correto "IdFilialResponsavel",
            // troque o setter abaixo para setIdFilialResponsavel(...)
            if (m.getIdFilialResponsavel() == null) m.setIdFilialResponsavel(props.getDefaults().getIdFilialResposavel());
            if (m.getIdEnderecoCidade() == null)   m.setIdEnderecoCidade(props.getDefaults().getIdEnderecoCidade());
            if (m.getHrColetaInicio() == null)     m.setHrColetaInicio(props.getDefaults().getHrInicio());
            if (m.getHrColetaFim() == null)        m.setHrColetaFim(props.getDefaults().getHrFim());
        }

        // Campos possivelmente obrigatórios (defina defaults seguros)
        if (m.getIdTipoColeta() == null) m.setIdTipoColeta(
            props.getDefaults() != null && props.getDefaults().getIdTipoColetaDefault() != null
                ? props.getDefaults().getIdTipoColetaDefault()
                : 1
        );
        if (m.getTpModal() == null) m.setTpModal(1); // ajuste se houver mapeamento de modal

        // Datas (LocalDate)
        LocalDate dtColeta  = r.getDtColetaPrev()  != null ? r.getDtColetaPrev().toLocalDate()  : null;
        LocalDate dtEntrega = r.getDtEntregaPrev() != null ? r.getDtEntregaPrev().toLocalDate() : null;
        m.setDtColeta(dtColeta);
        m.setDtEntrega(dtEntrega);

        // Endereço de coleta (origem)
        m.setCdEnderecoCEP(r.getOrigemCep());
        m.setDsEndereco(r.getOrigemEndereco());
        m.setNrEnderecoNR(r.getOrigemNumero());
        m.setDsEnderecoBairro(r.getOrigemBairro());
        m.setDsEnderecoComplento(r.getOrigemComplemento());

        // Contatos / referência
        m.setDsSolicitante(r.getSolicitante());
        m.setDsProcurarPor(r.getProcurarPor());
        m.setNrTelefone(r.getTelefone());
        m.setNrReferencia(nvl(r.getNrReferencia(), r.getPedidoCliente()));
        m.setNrPedidoCliente(r.getPedidoCliente());

        // NF (usa setNr / setVl do seu DTO atual)
        List<SalvaColetaNFModel> nfs = new ArrayList<>();
        if (r.getNotas() != null) {
            for (DtmPendingRow.Nota n : r.getNotas()) {
                if (n == null) continue;
                SalvaColetaNFModel nf = new SalvaColetaNFModel();
                nf.setNumero(n.getNumero());
                nf.setValor(toDbl(n.getValor()) != null ? toDbl(n.getValor()) : 0d); // double
                nfs.add(nf);
            }
        }
        m.setNF(nfs);

        // Dimensões (usa setQt / setComp / setLarg / setAlt / setKg)
        List<SalvaColetaDimensoesModel> dims = new ArrayList<>();
        if (r.getDimensoes() != null && !r.getDimensoes().isEmpty()) {
            for (DtmPendingRow.Dimensao d : r.getDimensoes()) {
                if (d == null) continue;
                SalvaColetaDimensoesModel dd = new SalvaColetaDimensoesModel();
                dd.setQtd(nzi(d.getQtd(), 1));
                dd.setComp(toDblFromCm(d.getCompCm())); // confirme se API quer cm (int) ou m (double)
                dd.setLarg(toDblFromCm(d.getLargCm()));
                dd.setAlt(toDblFromCm(d.getAltCm()));
                Double kg = firstNonNull(
                        toDbl(d.getKgTaxado()),
                        toDbl(d.getKgCubado()),
                        toDbl(d.getKgBruto()),
                        0d
                );
                dd.setKg(kg);
                dims.add(dd);
            }
        } else {
            // Fallback mínimo aceito
            SalvaColetaDimensoesModel dd = new SalvaColetaDimensoesModel();
            dd.setQtd(1);
            dd.setKg(0d);
            dims.add(dd);
        }
        m.setDimensoes(dims);

        m.setDsComentarios(r.getComentarios());
        return m;
    }

    // ===== helpers =====
    private static String nvl(String a, String b) {
        return (a != null && !a.trim().isEmpty()) ? a : b;
    }
    private static Integer nzi(Integer v, Integer def) { return v == null ? def : v; }
    private static Double toDbl(BigDecimal v) { return v == null ? null : v.doubleValue(); }
    private static Double toDblFromCm(Integer cm) { return cm == null ? null : cm / 100d; }
    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) if (v != null) return v;
        return null;
    }
}
