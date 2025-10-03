package com.tecnolog.autocoleta.dtm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.config.AppProperties;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DtmRepository {

    private final JdbcTemplate jdbc;
    private final AppProperties props;
    private final ObjectMapper om;

    public DtmRepository(JdbcTemplate jdbc, AppProperties props, ObjectMapper om) {
        this.jdbc = jdbc;
        this.props = props;
        this.om = om;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static OffsetDateTime toOffsetDateTime(LocalDate d) {
        if (d == null) return null;
        return d.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /** Checa se o ResultSet possui uma coluna (por label ou nome), case-insensitive. */
    private static boolean has(ResultSet rs, String label) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        for (int i = 1; i <= n; i++) {
            if (label.equalsIgnoreCase(md.getColumnLabel(i))) return true;
            if (label.equalsIgnoreCase(md.getColumnName(i))) return true;
        }
        return false;
    }

    // ====== RowMapper tolerante ======
    private final RowMapper<DtmPendingRow> mapper = new RowMapper<DtmPendingRow>() {
        @Override
        public DtmPendingRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            DtmPendingRow r = new DtmPendingRow();

            // IDs / datas
            if (has(rs, "id_dtm")) r.setIdDtm(rs.getLong("id_dtm"));

            LocalDate dtColeta = null;
            if (has(rs, "dt_coletaprevisao")) {
                dtColeta = rs.getObject("dt_coletaprevisao", LocalDate.class);
            }
            r.setDtColetaPrev(toOffsetDateTime(dtColeta));

            LocalDate dtEntrega = null;
            if (has(rs, "dt_entregaprevisao")) {
                dtEntrega = rs.getObject("dt_entregaprevisao", LocalDate.class);
            }
            r.setDtEntregaPrev(toOffsetDateTime(dtEntrega));

            if (has(rs, "dt_inclusao")) {
                r.setDtInclusao(rs.getObject("dt_inclusao", OffsetDateTime.class));
            }

            // Cabeçalho
            if (has(rs, "ds_nivel_servico")) r.setNivelServico(rs.getString("ds_nivel_servico"));
            if (has(rs, "ds_tipo_servico")) r.setTipoServico(rs.getString("ds_tipo_servico"));
            if (has(rs, "ds_filial_responsavel")) r.setFilialResponsavel(rs.getString("ds_filial_responsavel"));
            if (has(rs, "ds_agente")) r.setAgente(rs.getString("ds_agente"));
            if (has(rs, "ds_solicitante")) r.setSolicitante(rs.getString("ds_solicitante"));
            if (has(rs, "nr_referencia")) r.setNrReferencia(rs.getString("nr_referencia"));
            if (has(rs, "pedido_cliente")) r.setPedidoCliente(rs.getString("pedido_cliente"));
            if (has(rs, "comentarios")) r.setComentarios(rs.getString("comentarios"));

            if (has(rs, "vl_total")) r.setValorTotal(rs.getBigDecimal("vl_total"));

            // Contato/local coleta
            if (has(rs, "ds_local_coleta")) r.setLocalColetaNome(rs.getString("ds_local_coleta"));
            if (has(rs, "procurar_por")) r.setProcurarPor(rs.getString("procurar_por"));
            if (has(rs, "telefone")) r.setTelefone(rs.getString("telefone"));

            // Origem
            if (has(rs, "ds_origem_nome")) r.setOrigemNome(rs.getString("ds_origem_nome"));
            if (has(rs, "ds_origem_cep")) r.setOrigemCep(rs.getString("ds_origem_cep"));
            if (has(rs, "ds_origem_endereco")) r.setOrigemEndereco(rs.getString("ds_origem_endereco"));
            if (has(rs, "ds_origem_numero")) r.setOrigemNumero(rs.getString("ds_origem_numero"));
            if (has(rs, "ds_origem_bairro")) r.setOrigemBairro(rs.getString("ds_origem_bairro"));
            if (has(rs, "ds_origem_complemento")) r.setOrigemComplemento(rs.getString("ds_origem_complemento"));
            if (has(rs, "ds_origem_cidadeuf")) {
                String cidadeUf = rs.getString("ds_origem_cidadeuf");
                if (cidadeUf != null) {
                    String[] parts = cidadeUf.split("/");
                    r.setOrigemCidade(parts.length > 0 ? parts[0] : null);
                    r.setOrigemUf(parts.length > 1 ? parts[1] : null);
                }
            }

            // Destino — ler somente o que existir na view
            if (has(rs, "ds_destino_nome")) r.setDestinoNome(rs.getString("ds_destino_nome"));
            if (has(rs, "ds_destino_cep")) r.setDestinoCep(rs.getString("ds_destino_cep"));
            if (has(rs, "ds_destino_endereco")) r.setDestinoEndereco(rs.getString("ds_destino_endereco"));
            if (has(rs, "ds_destino_numero")) r.setDestinoNumero(rs.getString("ds_destino_numero"));
            if (has(rs, "ds_destino_bairro")) r.setDestinoBairro(rs.getString("ds_destino_bairro"));
            if (has(rs, "ds_destino_complemento")) r.setDestinoComplemento(rs.getString("ds_destino_complemento"));
            if (has(rs, "ds_destino_cidade")) r.setDestinoCidade(rs.getString("ds_destino_cidade"));
            if (has(rs, "ds_destino_uf")) r.setDestinoUf(rs.getString("ds_destino_uf"));

            // Defaults para natureza/embalagem (se a view expuser)
            if (has(rs, "ds_natureza_carga")) r.setNaturezaCarga(rs.getString("ds_natureza_carga"));
            if (has(rs, "ds_embalagem")) r.setEmbalagem(rs.getString("ds_embalagem"));

            // JSON: Notas Fiscais
            if (has(rs, "json_notas")) {
                String jsonNf = rs.getString("json_notas");
                if (!isBlank(jsonNf) && !"[]".equals(jsonNf)) {
                    try {
                        List<NF> raw = om.readValue(jsonNf, new TypeReference<List<NF>>() {});
                        List<DtmPendingRow.Nota> notas = new ArrayList<DtmPendingRow.Nota>();
                        for (NF n : raw) {
                            DtmPendingRow.Nota x = new DtmPendingRow.Nota();
                            x.setNumero(n.getNumero());
                            x.setSerie(n.getSerie());
                            x.setSubserie(n.getSubserie());
                            x.setValor(n.getValor());
                            x.setMoeda(n.getMoeda());
                            notas.add(x);
                        }
                        r.setNotas(notas);
                    } catch (Exception e) {
                        throw new SQLException("Falha ao parsear JSON de Notas Fiscais", e);
                    }
                }
            }

            // JSON: Dimensões
            if (has(rs, "json_dimensoes")) {
                String jsonDim = rs.getString("json_dimensoes");
                if (!isBlank(jsonDim) && !"[]".equals(jsonDim)) {
                    try {
                        List<DIM> raw = om.readValue(jsonDim, new TypeReference<List<DIM>>() {});
                        List<DtmPendingRow.Dimensao> dims = new ArrayList<DtmPendingRow.Dimensao>();
                        for (DIM d : raw) {
                            DtmPendingRow.Dimensao x = new DtmPendingRow.Dimensao();
                            x.setQtd(d.getQtd());
                            x.setCompCm(d.getComp_cm());
                            x.setLargCm(d.getLarg_cm());
                            x.setAltCm(d.getAlt_cm());
                            x.setKgBruto(d.getKg_bruto());
                            x.setKgCubado(d.getKg_cubado());
                            x.setKgTaxado(d.getKg_taxado());
                            x.setValor(d.getValor());
                            x.setNatureza(d.getNatureza());
                            x.setEmbalagem(d.getEmbalagem());
                            dims.add(x);
                        }
                        r.setDimensoes(dims);
                    } catch (Exception e) {
                        throw new SQLException("Falha ao parsear JSON de Dimensões", e);
                    }
                }
            }

            return r;
        }
    };

    /** Retorna a próxima DTM pendente (ainda não marcada como processed no lock). */
    public Optional<DtmPendingRow> fetchNextPending() {
        String view = props.getDtm().getView();
        String lock = props.getDtm().getLockTable();

        String sql =
            "SELECT " +
            "  v.\"DTM\"                AS id_dtm, " +
            "  v.\"Data Coleta\"        AS dt_coletaprevisao, " +
            "  v.\"Data Entrega\"       AS dt_entregaprevisao, " +
            "  now()                    AS dt_inclusao, " +
            "  v.\"Tipo de Coleta\"     AS ds_nivel_servico, " +
            "  v.\"Modal\"              AS ds_tipo_servico, " +
            "  v.\"Filial Responsável\" AS ds_filial_responsavel, " +
            "  v.\"Agente\"             AS ds_agente, " +
            "  v.\"Solicitante\"        AS ds_solicitante, " +
            "  v.\"Nº Referencia\"      AS nr_referencia, " +
            "  v.\"Pedido Cliente\"     AS pedido_cliente, " +
            "  v.\"Telefone\"           AS telefone, " +
            "  v.\"Procurar Por\"       AS procurar_por, " +
            "  v.\"Valor Total\"        AS vl_total, " +
            "  v.\"Comentários\"        AS comentarios, " +
            "  v.\"Local Coleta\"       AS ds_local_coleta, " +
            "  v.\"Remetente\"          AS ds_origem_nome, " +
            "  v.\"CEP\"                AS ds_origem_cep, " +
            "  v.\"Endereço\"           AS ds_origem_endereco, " +
            "  v.\"Nº\"                 AS ds_origem_numero, " +
            "  v.\"Bairro\"             AS ds_origem_bairro, " +
            "  v.\"Complemento\"        AS ds_origem_complemento, " +
            "  v.\"Cidade/UF\"          AS ds_origem_cidadeuf, " +
            "  v.\"Destinatario\"       AS ds_destino_nome, " +
            "  CAST(v.\"Notas Fiscais\" AS TEXT) AS json_notas, " +
            "  CAST(v.\"Dimensões\"     AS TEXT) AS json_dimensoes " +
            "FROM " + view + " v " +
            "WHERE NOT EXISTS (" +
            "  SELECT 1 FROM " + lock + " l WHERE l.id_dtm = v.\"DTM\" AND l.processed = TRUE" +
            ") " +
            "ORDER BY v.\"Data Coleta\" NULLS LAST, v.\"DTM\" ASC " +
            "LIMIT 1";

        List<DtmPendingRow> list = jdbc.query(sql, mapper);
        return list.isEmpty() ? Optional.<DtmPendingRow>empty() : Optional.of(list.get(0));
    }

    /** Busca um lote de DTM pendentes, ordenadas. */
    public List<DtmPendingRow> buscarPendentesOrdenado(int limit) {
        String view = props.getDtm().getView();
        String lock = props.getDtm().getLockTable();

        String sql =
            "SELECT " +
            "  v.\"DTM\"                AS id_dtm, " +
            "  v.\"Data Coleta\"        AS dt_coletaprevisao, " +
            "  v.\"Data Entrega\"       AS dt_entregaprevisao, " +
            "  now()                    AS dt_inclusao, " +
            "  v.\"Tipo de Coleta\"     AS ds_nivel_servico, " +
            "  v.\"Modal\"              AS ds_tipo_servico, " +
            "  v.\"Filial Responsável\" AS ds_filial_responsavel, " +
            "  v.\"Agente\"             AS ds_agente, " +
            "  v.\"Solicitante\"        AS ds_solicitante, " +
            "  v.\"Nº Referencia\"      AS nr_referencia, " +
            "  v.\"Pedido Cliente\"     AS pedido_cliente, " +
            "  v.\"Telefone\"           AS telefone, " +
            "  v.\"Procurar Por\"       AS procurar_por, " +
            "  v.\"Valor Total\"        AS vl_total, " +
            "  v.\"Comentários\"        AS comentarios, " +
            "  v.\"Local Coleta\"       AS ds_local_coleta, " +
            "  v.\"Remetente\"          AS ds_origem_nome, " +
            "  v.\"CEP\"                AS ds_origem_cep, " +
            "  v.\"Endereço\"           AS ds_origem_endereco, " +
            "  v.\"Nº\"                 AS ds_origem_numero, " +
            "  v.\"Bairro\"             AS ds_origem_bairro, " +
            "  v.\"Complemento\"        AS ds_origem_complemento, " +
            "  v.\"Cidade/UF\"          AS ds_origem_cidadeuf, " +
            "  v.\"Destinatario\"       AS ds_destino_nome, " +
            "  CAST(v.\"Notas Fiscais\" AS TEXT) AS json_notas, " +
            "  CAST(v.\"Dimensões\"     AS TEXT) AS json_dimensoes " +
            "FROM " + view + " v " +
            "WHERE NOT EXISTS (" +
            "  SELECT 1 FROM " + lock + " l WHERE l.id_dtm = v.\"DTM\" AND l.processed = TRUE" +
            ") " +
            "ORDER BY v.\"Data Coleta\" NULLS LAST, v.\"DTM\" ASC " +
            "LIMIT " + Math.max(1, limit);

        return jdbc.query(sql, mapper);
    }

    // ===== DTOs auxiliares para desserializar os JSONs =====
    private static class NF {
        private Integer numero;
        private Integer serie;
        private Integer subserie;
        private BigDecimal valor;
        private String moeda;

        public Integer getNumero() { return numero; }
        public void setNumero(Integer numero) { this.numero = numero; }
        public Integer getSerie() { return serie; }
        public void setSerie(Integer serie) { this.serie = serie; }
        public Integer getSubserie() { return subserie; }
        public void setSubserie(Integer subserie) { this.subserie = subserie; }
        public BigDecimal getValor() { return valor; }
        public void setValor(BigDecimal valor) { this.valor = valor; }
        public String getMoeda() { return moeda; }
        public void setMoeda(String moeda) { this.moeda = moeda; }
    }

    private static class DIM {
        private Integer qtd;
        private Integer comp_cm;
        private Integer larg_cm;
        private Integer alt_cm;
        private BigDecimal kg_bruto;
        private BigDecimal kg_cubado;
        private BigDecimal kg_taxado;
        private BigDecimal valor;
        private String natureza;
        private String embalagem;

        public Integer getQtd() { return qtd; }
        public void setQtd(Integer qtd) { this.qtd = qtd; }
        public Integer getComp_cm() { return comp_cm; }
        public void setComp_cm(Integer comp_cm) { this.comp_cm = comp_cm; }
        public Integer getLarg_cm() { return larg_cm; }
        public void setLarg_cm(Integer larg_cm) { this.larg_cm = larg_cm; }
        public Integer getAlt_cm() { return alt_cm; }
        public void setAlt_cm(Integer alt_cm) { this.alt_cm = alt_cm; }
        public BigDecimal getKg_bruto() { return kg_bruto; }
        public void setKg_bruto(BigDecimal kg_bruto) { this.kg_bruto = kg_bruto; }
        public BigDecimal getKg_cubado() { return kg_cubado; }
        public void setKg_cubado(BigDecimal kg_cubado) { this.kg_cubado = kg_cubado; }
        public BigDecimal getKg_taxado() { return kg_taxado; }
        public void setKg_taxado(BigDecimal kg_taxado) { this.kg_taxado = kg_taxado; }
        public BigDecimal getValor() { return valor; }
        public void setValor(BigDecimal valor) { this.valor = valor; }
        public String getNatureza() { return natureza; }
        public void setNatureza(String natureza) { this.natureza = natureza; }
        public String getEmbalagem() { return embalagem; }
        public void setEmbalagem(String embalagem) { this.embalagem = embalagem; }
    }
}
