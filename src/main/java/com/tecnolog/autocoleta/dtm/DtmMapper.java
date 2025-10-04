package com.tecnolog.autocoleta.dtm;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DtmMapper {

    public DtmMapper() {}

    private static final DateTimeFormatter API_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static String toApiDate(Object dt) {
        if (dt == null) return null;
        if (dt instanceof OffsetDateTime odt) return odt.toLocalDateTime().format(API_DT);
        if (dt instanceof LocalDateTime ldt)   return ldt.format(API_DT);
        if (dt instanceof LocalDate ld)        return ld.atStartOfDay().format(API_DT);
        if (dt instanceof java.util.Date d) {
            LocalDateTime ldt = Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return ldt.format(API_DT);
        }
        if (dt instanceof CharSequence cs) {
            String iso = cs.toString();
            try { return OffsetDateTime.parse(iso).toLocalDateTime().format(API_DT); } catch (Exception ignored) {}
            try { return LocalDateTime.parse(iso).format(API_DT); } catch (Exception ignored) {}
            try { return LocalDate.parse(iso).atStartOfDay().format(API_DT); } catch (Exception ignored) {}
            return iso;
        }
        return dt.toString();
    }

    public DtmJson toPedidos(DtmPendingRow r) {
        DtmJson j = new DtmJson();

        // CREATE: não enviar Id
        j.Id = null;
        j.Dtm = r.getIdDtm();
        j.Versao = (r.getNrVersao() != null ? r.getNrVersao() : 1);

        j.DtInicio = toApiDate(r.getDtColetaPrev());
        j.DtFim    = toApiDate(r.getDtEntregaPrev());

        j.NivelServico   = asciiNoAccents(r.getNivelServico());
        j.TipoServico    = r.getTipoServico();
        j.Transportadora = r.getFilialResponsavel();
        j.Analista       = r.getAgente();
        j.Solicitante    = r.getSolicitante();
        j.Referencia     = truncate(nz(nz(r.getNrReferencia(), r.getPedidoCliente()), "DTM-" + r.getIdDtm()), 40);
        j.Observacoes    = compact(r.getComentarios());

        if (r.getValorTotal() != null)
            j.ValorTotalComImposto = r.getValorTotal().doubleValue();

        // ORIGEM
        j.Origem = new DtmJson.Endpoint();
        j.Origem.Nome         = nz(r.getOrigemNome(), r.getLocalColetaNome());
        j.Origem.Cep          = r.getOrigemCep();
        j.Origem.Endereco     = r.getOrigemEndereco();
        j.Origem.Numero       = r.getOrigemNumero();
        j.Origem.Bairro       = r.getOrigemBairro();
        j.Origem.Complemento  = r.getOrigemComplemento();
        j.Origem.Cidade       = r.getOrigemCidade();
        j.Origem.UF           = r.getOrigemUf();
        j.Origem.ContatosOrigem = new ArrayList<>();
        DtmJson.Contato cO = new DtmJson.Contato();
        cO.Nome      = nz(r.getProcurarPor(), nz(r.getSolicitante(), "Contato"));
        cO.Telefone  = nz(onlyDigits(r.getTelefone()), "000000000");
        cO.Email     = nz(r.getSolicitante(), "");
        j.Origem.ContatosOrigem.add(cO);

        // DESTINO (+ fallback Jacarepaguá)
        j.Destino = new DtmJson.Endpoint();
        j.Destino.Nome        = r.getDestinoNome();
        j.Destino.Cep         = r.getDestinoCep();
        j.Destino.Endereco    = r.getDestinoEndereco();
        j.Destino.Numero      = r.getDestinoNumero();
        j.Destino.Bairro      = r.getDestinoBairro();
        j.Destino.Complemento = r.getDestinoComplemento();
        j.Destino.Cidade      = r.getDestinoCidade();
        j.Destino.UF          = r.getDestinoUf();
        j.Destino.ContatosDestino = new ArrayList<>();
        // sempre manda 1 contato também no destino (muitos backends exigem)
        DtmJson.Contato cD = new DtmJson.Contato();
        cD.Nome     = nz(r.getProcurarPor(), nz(r.getSolicitante(), "Contato"));
        cD.Telefone = "000000000";
        cD.Email    = nz(r.getSolicitante(), "");
        j.Destino.ContatosDestino.add(cD);

        aplicarDestinoFallbackSeNecessario(j.Destino);

        // NOTAS
        j.NotasFiscaisRelacionadas = new ArrayList<>();
        if (r.getNotas() != null) {
            for (DtmPendingRow.Nota nf : r.getNotas()) {
                if (nf == null) continue;
                DtmJson.Nota n = new DtmJson.Nota();
                n.Numero   = nf.getNumero();
                n.Serie    = nf.getSerie();
                n.Subserie = nf.getSubserie();
                n.Valor    = nf.getValor() != null ? nf.getValor().doubleValue() : null;
                j.NotasFiscaisRelacionadas.add(n);
            }
        }
        if (j.NotasFiscaisRelacionadas.isEmpty()) {
            j.NotasFiscaisRelacionadas = null; // evita array vazio
        }

        // CARGAS
        j.Cargas = new ArrayList<>();
        if (r.getDimensoes() != null && !r.getDimensoes().isEmpty()) {
            for (DtmPendingRow.Dimensao d : r.getDimensoes()) {
                if (d == null) continue;
                DtmJson.Carga cg = new DtmJson.Carga();
                cg.Descricao     = nz(d.getNatureza(), nz(r.getNaturezaCarga(), "MERCADORIA"));
                cg.Embalagem     = nz(d.getEmbalagem(), r.getEmbalagem());
                cg.Quantidade    = nzi(d.getQtd(), 1);
                cg.Comp          = toMeters(d.getCompCm());
                cg.Larg          = toMeters(d.getLargCm());
                cg.Alt           = toMeters(d.getAltCm());
                cg.PesoBruto     = toDbl(d.getKgBruto());
                cg.PesoCubado    = toDbl(d.getKgCubado());
                cg.PesoTaxado    = toDbl(d.getKgTaxado());
                cg.ValorMaterial = j.ValorTotalComImposto != null ? j.ValorTotalComImposto : 0d;
                j.Cargas.add(cg);
            }
        }

        // Se nenhuma carga tem dimensões/pesos, tenta extrair das observações; senão zera.
        boolean temMetricas = j.Cargas.stream().anyMatch(c ->
                c.Comp != null || c.Larg != null || c.Alt != null ||
                c.PesoBruto != null || c.PesoCubado != null || c.PesoTaxado != null);

        if (!temMetricas) {
            double[] dims = extrairDimsDeObservacoes(j.Observacoes);
            if (dims != null) {
                if (j.Cargas.isEmpty()) {
                    DtmJson.Carga cg = baseCarga(r, j);
                    cg.Comp = dims[0]; cg.Larg = dims[1]; cg.Alt = dims[2];
                    j.Cargas.add(cg);
                } else {
                    // coloca ao menos na primeira carga
                    DtmJson.Carga cg = j.Cargas.get(0);
                    cg.Descricao = nz(cg.Descricao, nz(r.getNaturezaCarga(), "MERCADORIA"));
                    cg.Comp = dims[0]; cg.Larg = dims[1]; cg.Alt = dims[2];
                }
            } else {
                if (j.Cargas.isEmpty()) {
                    DtmJson.Carga cg = baseCarga(r, j);
                    cg.Comp = 0.0; cg.Larg = 0.0; cg.Alt = 0.0;
                    j.Cargas.add(cg);
                } else {
                    for (DtmJson.Carga cg : j.Cargas) {
                        if (cg.Comp == null && cg.Larg == null && cg.Alt == null) {
                            cg.Comp = 0.0; cg.Larg = 0.0; cg.Alt = 0.0;
                        }
                        cg.Descricao = nz(cg.Descricao, nz(r.getNaturezaCarga(), "MERCADORIA"));
                    }
                }
            }
        } else {
            // assegura Descricao em todas
            for (DtmJson.Carga cg : j.Cargas) {
                cg.Descricao = nz(cg.Descricao, nz(r.getNaturezaCarga(), "MERCADORIA"));
            }
        }

        return j;
    }

    private static DtmJson.Carga baseCarga(DtmPendingRow r, DtmJson j){
        DtmJson.Carga cg = new DtmJson.Carga();
        cg.Descricao     = nz(r.getNaturezaCarga(), "MERCADORIA");
        cg.Embalagem     = nz(r.getEmbalagem(), "CAIXA");
        cg.Quantidade    = 1;
        cg.PesoBruto = cg.PesoCubado = cg.PesoTaxado = null;
        cg.ValorMaterial = j.ValorTotalComImposto != null ? j.ValorTotalComImposto : 0d;
        return cg;
    }

    /* ========================= helpers ========================= */

    private static String asciiNoAccents(String s) {
        if (s == null) return null;
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }

    private static String nz(String s, String def){ return (s == null || s.trim().isEmpty()) ? def : s; }

    private static String truncate(String s, int max){
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String compact(String s){
        if (s == null) return null;
        // remove quebras e espaços duplicados (muitos backends legados agradecem)
        return s.replace('\r',' ')
                .replace('\n',' ')
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static String onlyDigits(String s){
        return (s == null) ? null : s.replaceAll("\\D+", "");
    }

    private static Integer nzi(Integer v, Integer def){ return v == null ? def : v; }

    private static Double toMeters(Integer cm){ return cm == null ? null : (cm / 100d); }

    private static Double toDbl(java.math.BigDecimal v){ return v == null ? null : v.doubleValue(); }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static void aplicarDestinoFallbackSeNecessario(DtmJson.Endpoint d) {
        if (d == null || d.Nome == null) return;
        String nome = d.Nome.trim().toUpperCase(Locale.ROOT);
        if (nome.contains("JACAREPAGUA")) {
            if (isBlank(d.Cep))        d.Cep = "22775002";
            if (isBlank(d.Endereco))   d.Endereco = "AV AYRTON SENNA";
            if (isBlank(d.Numero))     d.Numero = "2541";
            if (isBlank(d.Bairro))     d.Bairro = "BARRA DA TIJUCA";
            if (isBlank(d.Cidade))     d.Cidade = "Rio de Janeiro";
            if (isBlank(d.UF))         d.UF = "RJ";
            if (isBlank(d.Complemento)) d.Complemento = "FUNCIONAMENTO - 06:00 AS 17:00 HORAS";
        }
    }

    private static double[] extrairDimsDeObservacoes(String obs) {
        if (obs == null) return null;
        Pattern p = Pattern.compile("(\\d+,\\d+)\\s*[xX]\\s*(\\d+,\\d+)\\s*[xX]\\s*(\\d+,\\d+)");
        Matcher m = p.matcher(obs);
        if (!m.find()) return null;
        try {
            double comp = parsePtDouble(m.group(1));
            double larg = parsePtDouble(m.group(2));
            double alt  = parsePtDouble(m.group(3));
            return new double[]{comp, larg, alt};
        } catch (Exception ignore) { return null; }
    }

    private static double parsePtDouble(String s) { return Double.parseDouble(s.replace(",", ".")); }
}
