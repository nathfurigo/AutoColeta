package com.tecnolog.autocoleta.dtm;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Component
public class DtmMapper {

    public DtmMapper() {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public DtmJson toPedidos(DtmPendingRow r) {
        DtmJson j = new DtmJson();

        j.Id = r.getIdDtm() != null ? String.valueOf(r.getIdDtm()) : null;
        j.Dtm = r.getIdDtm();
        j.Versao = (r.getNrVersao() != null ? r.getNrVersao() : 1);
        j.DtInicio = fmt(r.getDtColetaPrev());
        j.DtFim = fmt(r.getDtEntregaPrev());
        j.NivelServico = normalizaEnum(r.getNivelServico());
        j.TipoServico = normalizaEnum(r.getTipoServico());
        j.Transportadora = r.getFilialResponsavel();
        j.Analista = r.getAgente();
        j.Solicitante = r.getSolicitante();
        j.Referencia = nz(r.getNrReferencia(), r.getPedidoCliente());
        j.Observacoes = r.getComentarios();
        if (r.getValorTotal() != null)
            j.ValorTotalComImposto = r.getValorTotal().doubleValue();

        j.Origem = new DtmJson.Endpoint();
        j.Origem.Nome = nz(r.getOrigemNome(), r.getLocalColetaNome());
        j.Origem.Cep = r.getOrigemCep();
        j.Origem.Endereco = r.getOrigemEndereco();
        j.Origem.Numero = r.getOrigemNumero();
        j.Origem.Bairro = r.getOrigemBairro();
        j.Origem.Complemento = r.getOrigemComplemento();
        j.Origem.Cidade = r.getOrigemCidade();
        j.Origem.UF = r.getOrigemUf();

        j.Origem.ContatosOrigem = new ArrayList<>();
        DtmJson.Contato cO = new DtmJson.Contato();
        cO.Nome = r.getProcurarPor();
        cO.Telefone = r.getTelefone();
        cO.Email = r.getSolicitante();
        j.Origem.ContatosOrigem.add(cO);

        j.Destino = new DtmJson.Endpoint();
        j.Destino.Nome = r.getDestinoNome();
        j.Destino.Cep = r.getDestinoCep();
        j.Destino.Endereco = r.getDestinoEndereco();
        j.Destino.Numero = r.getDestinoNumero();
        j.Destino.Bairro = r.getDestinoBairro();
        j.Destino.Complemento = r.getDestinoComplemento();
        j.Destino.Cidade = r.getDestinoCidade();
        j.Destino.UF = r.getDestinoUf();
        j.Destino.ContatosDestino = new ArrayList<>();
        // Garante ao menos um contato de destino (fallback no solicitante)
        DtmJson.Contato cD = new DtmJson.Contato();
        cD.Nome = nz(r.getProcurarPor(), "Contato Destino");
        cD.Telefone = r.getTelefone();
        cD.Email = r.getSolicitante();
        j.Destino.ContatosDestino.add(cD);

        j.NotasFiscaisRelacionadas = new ArrayList<>();
        if (r.getNotas() != null) {
            for (DtmPendingRow.Nota nf : r.getNotas()) {
                if (nf == null) continue;
                DtmJson.Nota n = new DtmJson.Nota();
                n.Numero = nf.getNumero();
                n.Serie = nf.getSerie();
                n.Subserie = nf.getSubserie();
                n.Valor = nf.getValor() != null ? nf.getValor().doubleValue() : null;
                j.NotasFiscaisRelacionadas.add(n);
            }
        }

        j.Cargas = new ArrayList<>();
        if (r.getDimensoes() != null) {
            for (DtmPendingRow.Dimensao d : r.getDimensoes()) {
                if (d == null) continue;
                DtmJson.Carga cg = new DtmJson.Carga();
                cg.Descricao = nz(d.getNatureza(), r.getNaturezaCarga());
                cg.Embalagem = nz(nz(d.getEmbalagem(), r.getEmbalagem()), "CAIXA");
                cg.Quantidade = nzi(d.getQtd(), 1);
                cg.Comp = toMeters(d.getCompCm());
                cg.Larg = toMeters(d.getLargCm());
                cg.Alt  = toMeters(d.getAltCm());
                cg.PesoBruto  = toDbl(d.getKgBruto());
                cg.PesoCubado = toDbl(d.getKgCubado());
                cg.PesoTaxado = toDbl(d.getKgTaxado());
                cg.ValorMaterial = j.ValorTotalComImposto != null ? j.ValorTotalComImposto : 0d;
                j.Cargas.add(cg);
            }
        }

        return j;
    }

    private static String nz(String s, String def){
        return (s == null || s.trim().isEmpty()) ? def : s;
    }

    private static Integer nzi(Integer v, Integer def){
        return v == null ? def : v;
    }

    private static Double toMeters(Integer cm){
        return cm == null ? null : (cm / 100d);
    }

    private static Double toDbl(java.math.BigDecimal v){
        return v == null ? null : v.doubleValue();
    }

    private static String fmt(OffsetDateTime odt){
        return odt == null ? null : odt.toLocalDateTime().format(FMT);
    }

    private static String normalizaEnum(String valor){
        if (valor == null) return null;
        String ascii = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return ascii.toUpperCase().trim();
    }
}
