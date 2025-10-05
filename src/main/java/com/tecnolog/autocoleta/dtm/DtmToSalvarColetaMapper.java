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

        // Id DTM
        m.setIdDtm(r.getIdDtm());

        // Defaults do application.yml
        if (props.getDefaults() != null) {
            if (m.getIdRemetente() == null)         m.setIdRemetente(props.getDefaults().getIdRemetente());
            if (m.getIdDestinatario() == null)      m.setIdDestinatario(props.getDefaults().getIdDestinatario());
            if (m.getIdTomador() == null)           m.setIdTomador(props.getDefaults().getIdTomador());
            if (m.getIdLocalColeta() == null)       m.setIdLocalColeta(props.getDefaults().getIdLocalColeta());
            // AppProperties tem "idFilialResposavel" (typo no nome do getter)
            if (m.getIdFilialResponsavel() == null) m.setIdFilialResponsavel(props.getDefaults().getIdFilialResposavel());
            if (m.getHrColetaInicio() == null)      m.setHrColetaInicio(props.getDefaults().getHrInicio());
            if (m.getHrColetaFim() == null)         m.setHrColetaFim(props.getDefaults().getHrFim());

            Integer idCid = props.getDefaults().getIdEnderecoCidade();
            if (m.getIdEnderecoCidade() == null && idCid != null && idCid > 0 && idCid != 999) {
                m.setIdEnderecoCidade(idCid);
            }
        }

        // Campos possivelmente obrigatórios
        if (m.getIdTipoColeta() == null) {
            Integer def = props.getDefaults() != null ? props.getDefaults().getIdTipoColetaDefault() : null;
            m.setIdTipoColeta(def != null ? def : 1);
        }
        if (m.getTpModal() == null) m.setTpModal(1);

        // Datas
        LocalDate dtColeta  = r.getDtColetaPrev()  != null ? r.getDtColetaPrev().toLocalDate()  : null;
        LocalDate dtEntrega = r.getDtEntregaPrev() != null ? r.getDtEntregaPrev().toLocalDate() : null;
        m.setDtColeta(dtColeta);
        m.setDtEntrega(dtEntrega);

        // Endereço de coleta (origem)
        m.setCdEnderecoCEP(r.getOrigemCep());
        m.setDsEndereco(r.getOrigemEndereco());
        m.setNrEnderecoNR(r.getOrigemNumero());
        m.setDsEnderecoBairro(r.getOrigemBairro());
        // setter de compatibilidade aponta para DsEnderecoComplemento
        m.setDsEnderecoComplento(r.getOrigemComplemento());

        // Contatos / referência
        m.setDsSolicitante(r.getSolicitante());
        m.setDsProcurarPor(r.getProcurarPor());
        m.setNrTelefone(r.getTelefone());
        m.setNrReferencia(nvl(r.getNrReferencia(), r.getPedidoCliente()));
        m.setNrPedidoCliente(r.getPedidoCliente());

        // -------- Notas Fiscais (sem Id/Chave) --------
        List<SalvaColetaNFModel> nfs = new ArrayList<>();
        if (r.getNotas() != null) {
            for (DtmPendingRow.Nota n : r.getNotas()) {
                if (n == null) continue;
                SalvaColetaNFModel nf = new SalvaColetaNFModel();
                // NÃO existe getId()/getChave() nessa Nota
                // nf.setId(...);
                // nf.setChave(...);

                nf.setNumero(toInt(n.getNumero())); // Integer
                nf.setSerie(toInt(n.getSerie()));   // Integer
                nf.setValor(toDbl(n.getValor()));   // Double

                nfs.add(nf);
            }
        }
        m.setNF(nfs);

        // -------- Dimensões --------
        List<SalvaColetaDimensoesModel> dims = new ArrayList<>();
        if (r.getDimensoes() != null && !r.getDimensoes().isEmpty()) {
            for (DtmPendingRow.Dimensao d : r.getDimensoes()) {
                if (d == null) continue;
                SalvaColetaDimensoesModel dd = new SalvaColetaDimensoesModel();
                dd.setQtd(nzi(d.getQtd(), 1));

                // Peso: prioriza Taxado > Cubado > Bruto
                Double kg = firstNonNull(
                        toDbl(d.getKgTaxado()),
                        toDbl(d.getKgCubado()),
                        toDbl(d.getKgBruto()),
                        0d
                );
                dd.setKg(kg);

                // (Opcional) Preencher medidas físicas a partir de ...Cm
                // Se quiser em METROS, troque para (d.getCompCm() / 100.0) etc.
                if (d.getCompCm() != null) dd.setComprimento(d.getCompCm().doubleValue());
                if (d.getLargCm() != null) dd.setLargura(d.getLargCm().doubleValue());
                if (d.getAltCm()  != null) dd.setAltura(d.getAltCm().doubleValue());

                dims.add(dd);
            }
        } else {
            SalvaColetaDimensoesModel dd = new SalvaColetaDimensoesModel();
            dd.setQtd(1);
            dd.setKg(0d);
            dims.add(dd);
        }
        m.setDimensoes(dims);

        return m;
    }

    // helpers
    private static String nvl(String a, String b) { return (a != null && !a.trim().isEmpty()) ? a : b; }
    private static Integer nzi(Integer v, Integer def) { return v == null ? def : v; }
    private static Integer toInt(Number n) { return n == null ? null : n.intValue(); }
    private static Double toDbl(BigDecimal v) { return v == null ? null : v.doubleValue(); }
    @SafeVarargs
    private static <T> T firstNonNull(T... values) { for (T v : values) if (v != null) return v; return null; }
}
