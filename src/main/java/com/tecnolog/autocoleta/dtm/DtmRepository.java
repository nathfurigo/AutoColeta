package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class DtmRepository {
    private final JdbcTemplate jdbc;
    private final AppProperties props;

    public DtmRepository(JdbcTemplate jdbc, AppProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    // ATENÇÃO: mapeia somente as colunas que o DtmMapper usa hoje
    private final RowMapper<DtmPendingRow> mapper = new RowMapper<DtmPendingRow>() {
        @Override
        public DtmPendingRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            DtmPendingRow r = new DtmPendingRow();
            r.setIdDtm(rs.getLong("id_dtm"));

            // Se sua coluna no banco é timestamp/timestamptz, este getObject funciona.
            r.setDtColetaPrev(rs.getObject("dt_coletaprevisao", OffsetDateTime.class));
            r.setDtEntregaPrev(rs.getObject("dt_entregaprevisao", OffsetDateTime.class));
            r.setDtInclusao(rs.getObject("dt_inclusao", OffsetDateTime.class));

            r.setNivelServico(rs.getString("ds_nivel_servico"));

            // Origem (coleta) — vêm da view (colunas com espaço) via aliases
            r.setOrigemNome(rs.getString("ds_origem_nome"));
            r.setOrigemCep(rs.getString("ds_origem_cep"));
            r.setOrigemEndereco(rs.getString("ds_origem_endereco"));
            r.setOrigemNumero(rs.getString("ds_origem_numero"));
            r.setOrigemBairro(rs.getString("ds_origem_bairro"));
            r.setOrigemCompl(rs.getString("ds_origem_complemento"));

            // Campos que a view não expõe (CNPJ, cidade/UF separadas, etc.) ficam nulos por enquanto.
            r.setOrigemCidade(null);
            r.setOrigemUf(null);
            r.setDestinoNome(rs.getString("ds_destino_nome")); // se quiser usar depois

            return r;
        }
    };

    public Optional<DtmPendingRow> fetchNextPending() {
        String view = props.getDtm().getView();
        String lock = props.getDtm().getLockTable();

        // Seleciona da view (com nomes “bonitos”) e ALIAS para nomes técnicos (sem espaço)
        String sql =
            "SELECT \n" +
            "  v.\"DTM\"              AS id_dtm,\n" +
            "  v.\"Data Coleta\"      AS dt_coletaprevisao,\n" +
            "  v.\"Data Entrega\"     AS dt_entregaprevisao,\n" +
            "  now()                  AS dt_inclusao,\n" +
            "  v.\"Tipo de Coleta\"   AS ds_nivel_servico,\n" +
            "  v.\"Remetente\"        AS ds_origem_nome,\n" +
            "  v.\"CEP\"              AS ds_origem_cep,\n" +
            "  v.\"Endereço\"         AS ds_origem_endereco,\n" +
            "  v.\"Nº\"               AS ds_origem_numero,\n" +
            "  v.\"Bairro\"           AS ds_origem_bairro,\n" +
            "  v.\"Complemento\"      AS ds_origem_complemento,\n" +
            "  v.\"Destinatario\"     AS ds_destino_nome\n" +
            "FROM " + view + " v\n" +
            "WHERE NOT EXISTS (\n" +
            "  SELECT 1 FROM " + lock + " l\n" +
            "  WHERE l.id_dtm = v.\"DTM\" AND l.processed = TRUE\n" +
            ")\n" +
            "ORDER BY COALESCE(v.prioridade_ordem, 9), v.\"Data Coleta\" NULLS LAST, v.\"DTM\"\n" +
            "LIMIT 1";

        return jdbc.query(sql, mapper).stream().findFirst();
    }
}
