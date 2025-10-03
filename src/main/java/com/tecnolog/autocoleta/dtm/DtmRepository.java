package com.tecnolog.autocoleta.dtm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.config.AppProperties;

import lombok.Data;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate; // Importação adicionada
import java.time.OffsetDateTime;
import java.time.ZoneId; // Importação adicionada
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Data
public class DtmRepository {
    private final JdbcTemplate jdbc;
    private final AppProperties props;
    private final ObjectMapper om = new ObjectMapper();

    public DtmRepository(JdbcTemplate jdbc, AppProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    // Método auxiliar para converter LocalDate para OffsetDateTime
    private static OffsetDateTime toOffsetDateTime(LocalDate localDate) {
        if (localDate == null) return null;
        // Converte para OffsetDateTime usando o fuso horário padrão do sistema
        return localDate.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private final RowMapper<DtmPendingRow> mapper = new RowMapper<>() {
        @Override
        public DtmPendingRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            DtmPendingRow r = new DtmPendingRow();

            r.setIdDtm(rs.getLong("id_dtm"));
            
            // CORREÇÃO 1: Lê como LocalDate e converte para OffsetDateTime
            LocalDate dtColeta = rs.getObject("dt_coletaprevisao", LocalDate.class);
            r.setDtColetaPrev(toOffsetDateTime(dtColeta));

            // CORREÇÃO 1: Lê como LocalDate e converte para OffsetDateTime
            LocalDate dtEntrega = rs.getObject("dt_entregaprevisao", LocalDate.class);
            r.setDtEntregaPrev(toOffsetDateTime(dtEntrega));
            
            // dt_inclusao (assumindo que esta coluna é um TIMESTAMP/TIMESTAMPTZ válido)
            r.setDtInclusao(rs.getObject("dt_inclusao", OffsetDateTime.class));

            r.setNivelServico(rs.getString("ds_nivel_servico"));
            r.setTipoServico(rs.getString("ds_tipo_servico"));
            r.setFilialResponsavel(rs.getString("ds_filial_responsavel"));
            r.setAgente(rs.getString("ds_agente"));
            r.setSolicitante(rs.getString("ds_solicitante"));
            r.setNrReferencia(rs.getString("nr_referencia"));
            r.setPedidoCliente(rs.getString("pedido_cliente"));
            r.setComentarios(rs.getString("comentarios"));

            BigDecimal vlTotal = rs.getBigDecimal("vl_total");
            r.setValorTotal(vlTotal);

            // Contato/Local coleta
            r.setLocalColetaNome(rs.getString("ds_local_coleta"));

            r.setProcurarPor(rs.getString("procurar_por"));
            r.setTelefone(rs.getString("telefone"));

            // Origem
            r.setOrigemNome(rs.getString("ds_origem_nome"));
            r.setOrigemCep(rs.getString("ds_origem_cep"));
            r.setOrigemEndereco(rs.getString("ds_origem_endereco"));
            r.setOrigemNumero(rs.getString("ds_origem_numero"));
            r.setOrigemBairro(rs.getString("ds_origem_bairro"));
            r.setOrigemComplemento(rs.getString("ds_origem_complemento"));

            // Cidade/UF vem junto -> split
            String cidadeUf = rs.getString("ds_origem_cidadeuf"); 
            if (cidadeUf != null) {
                String[] parts = cidadeUf.split("/");
                r.setOrigemCidade(parts.length > 0 ? parts[0] : null);
                r.setOrigemUf(parts.length > 1 ? parts[1] : null);
            }

            // Destino
            r.setDestinoNome(rs.getString("ds_destino_nome"));

            // Natureza/Embalagem default para fallback de carga
            r.setNaturezaCarga(rs.getString("ds_natureza_carga"));
            r.setEmbalagem(rs.getString("ds_embalagem"));

            // JSONs: Notas Fiscais e Dimensões
            String jsonNf = rs.getString("json_notas");
            if (jsonNf != null && !jsonNf.isBlank() && !"[]".equals(jsonNf)) {
                try {
                    // view traz: numero, serie, subserie, valor
                    record NF(Integer numero, Integer serie, Integer subserie, BigDecimal valor, String moeda) {}
                    List<NF> raw = om.readValue(jsonNf, new TypeReference<List<NF>>(){});
                    List<DtmPendingRow.Nota> notas = new ArrayList<>();
                    for (NF n : raw) {
                        DtmPendingRow.Nota x = new DtmPendingRow.Nota();
                        x.setNumero(n.numero());
                        x.setSerie(n.serie());
                        x.setSubserie(n.subserie());
                        x.setValor(n.valor());
                        notas.add(x);
                    }
                    r.setNotas(notas);
                } catch (Exception e) {
                    throw new SQLException("Falha ao parsear JSON de Notas Fiscais", e);
                }
            }

            String jsonDim = rs.getString("json_dimensoes");
            if (jsonDim != null && !jsonDim.isBlank() && !"[]".equals(jsonDim)) {
                try {
                    record DIM(Integer qtd, Integer comp_cm, Integer larg_cm, Integer alt_cm,
                               BigDecimal kg_bruto, BigDecimal kg_cubado, BigDecimal kg_taxado,
                               BigDecimal valor, String natureza, String embalagem) {}
                    List<DIM> raw = om.readValue(jsonDim, new TypeReference<List<DIM>>(){});
                    List<DtmPendingRow.Dimensao> dims = new ArrayList<>();
                    for (DIM d : raw) {
                        DtmPendingRow.Dimensao x = new DtmPendingRow.Dimensao();
                        x.setQtd(d.qtd());
                        x.setCompCm(d.comp_cm());
                        x.setLargCm(d.larg_cm());
                        x.setAltCm(d.alt_cm());
                        x.setKgBruto(d.kg_bruto());
                        x.setKgCubado(d.kg_cubado());
                        x.setKgTaxado(d.kg_taxado());
                        x.setValor(d.valor());
                        x.setNatureza(d.natureza());
                        x.setEmbalagem(d.embalagem());
                        dims.add(x);
                    }
                    r.setDimensoes(dims);
                } catch (Exception e) {
                    throw new SQLException("Falha ao parsear JSON de Dimensões", e);
                }
            }

            return r;
        }
    };

    public Optional<DtmPendingRow> fetchNextPending() {
        String view = props.getDtm().getView();
        String lock = props.getDtm().getLockTable();

        String sql =
            "SELECT \n" +
            "  v.\"DTM\"                AS id_dtm,\n" +
            "  v.\"Data Coleta\"        AS dt_coletaprevisao,\n" +
            "  v.\"Data Entrega\"       AS dt_entregaprevisao,\n" +
            "  now()                    AS dt_inclusao,\n" +
            "  v.\"Tipo de Coleta\"     AS ds_nivel_servico,\n" +
            "  v.\"Modal\"              AS ds_tipo_servico,\n" +
            "  v.\"Filial Responsável\" AS ds_filial_responsavel,\n" +
            "  v.\"Agente\"             AS ds_agente,\n" +
            "  v.\"Solicitante\"        AS ds_solicitante,\n" +
            "  v.\"Nº Referencia\"      AS nr_referencia,\n" +
            "  v.\"Pedido Cliente\"     AS pedido_cliente,\n" +
            "  v.\"Telefone\"           AS telefone,\n" +
            "  v.\"Procurar Por\"       AS procurar_por,\n" +
            "  v.\"Valor Total\"        AS vl_total,\n" +
            "  v.\"Comentários\"        AS comentarios,\n" +
            "  v.\"Local Coleta\"       AS ds_local_coleta,\n" +
            "  v.\"Remetente\"          AS ds_origem_nome,\n" +
            "  v.\"CEP\"                AS ds_origem_cep,\n" +
            "  v.\"Endereço\"           AS ds_origem_endereco,\n" +
            "  v.\"Nº\"                 AS ds_origem_numero,\n" +
            "  v.\"Bairro\"             AS ds_origem_bairro,\n" +
            "  v.\"Complemento\"        AS ds_origem_complemento,\n" +
            "  v.\"Cidade/UF\"          AS ds_origem_cidadeuf,\n" +
            "  v.\"Destinatario\"       AS ds_destino_nome,\n" +
            "  v.\"Natureza da Carga\"  AS ds_natureza_carga,\n" +
            "  v.\"Embalagem\"          AS ds_embalagem,\n" +
            "  CAST(v.\"Notas Fiscais\" AS TEXT)  AS json_notas,\n" +
            "  CAST(v.\"Dimensões\"     AS TEXT)  AS json_dimensoes\n" +
            "FROM " + view + " v\n" +
            "WHERE NOT EXISTS (\n" +
            "  SELECT 1 FROM " + lock + " l\n" +
            "  WHERE l.id_dtm = v.\"DTM\" AND l.processed = TRUE\n" +
            ")\n" +
            "ORDER BY COALESCE(v.prioridade_ordem, 9), v.\"Data Coleta\" NULLS LAST, v.\"DTM\"\n" +
            "LIMIT 1";

        return jdbc.query(sql, mapper).stream().findFirst();
    }

    public List<DtmPendingRow> buscarPendentesOrdenado() {
        String view = props.getDtm().getView();
        String lock = props.getDtm().getLockTable();

        String sql =
            "SELECT \n" +
            "  v.\"DTM\"                AS id_dtm,\n" +
            "  v.\"Data Coleta\"        AS dt_coletaprevisao,\n" +
            "  v.\"Data Entrega\"       AS dt_entregaprevisao,\n" +
            "  now()                    AS dt_inclusao,\n" +
            "  v.\"Tipo de Coleta\"     AS ds_nivel_servico,\n" +
            "  v.\"Modal\"              AS ds_tipo_servico,\n" +
            "  v.\"Filial Responsável\" AS ds_filial_responsavel,\n" +
            "  v.\"Agente\"             AS ds_agente,\n" +
            "  v.\"Solicitante\"        AS ds_solicitante,\n" +
            "  v.\"Nº Referencia\"      AS nr_referencia,\n" +
            "  v.\"Pedido Cliente\"     AS pedido_cliente,\n" +
            "  v.\"Telefone\"           AS telefone,\n" +
            "  v.\"Procurar Por\"       AS procurar_por,\n" +
            "  v.\"Valor Total\"        AS vl_total,\n" +
            "  v.\"Comentários\"        AS comentarios,\n" +
            "  v.\"Local Coleta\"       AS ds_local_coleta,\n" +
            "  v.\"Remetente\"          AS ds_origem_nome,\n" +
            "  v.\"CEP\"                AS ds_origem_cep,\n" +
            "  v.\"Endereço\"           AS ds_origem_endereco,\n" +
            "  v.\"Nº\"                 AS ds_origem_numero,\n" +
            "  v.\"Bairro\"             AS ds_origem_bairro,\n" +
            "  v.\"Complemento\"        AS ds_origem_complemento,\n" +
            "  v.\"Cidade/UF\"          AS ds_origem_cidadeuf,\n" +
            "  v.\"Destinatario\"       AS ds_destino_nome,\n" +
            "  v.\"Natureza da Carga\"  AS ds_natureza_carga,\n" +
            "  v.\"Embalagem\"          AS ds_embalagem,\n" +
            "  CAST(v.\"Notas Fiscais\" AS TEXT)  AS json_notas,\n" +
            "  CAST(v.\"Dimensões\"     AS TEXT)  AS json_dimensoes\n" +
            "FROM " + view + " v\n" +
            "WHERE NOT EXISTS (\n" +
            "  SELECT 1 FROM " + lock + " l\n" +
            "  WHERE l.id_dtm = v.\"DTM\" AND l.processed = TRUE\n" +
            ")\n" +
            "ORDER BY COALESCE(v.prioridade_ordem, 9), v.\"Data Coleta\" NULLS LAST, v.\"DTM\"";

        return jdbc.query(sql, mapper);
    }
}
