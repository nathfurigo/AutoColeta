package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class DtmRepository {

    private final JdbcTemplate jdbc;
    private final String dtmView;
    private final String lockTable;

    public DtmRepository(@Qualifier("postgresJdbcTemplate") JdbcTemplate jdbc, AppProperties props) {
        this.jdbc = jdbc;
        this.dtmView = props.getDtm().getView();
        this.lockTable = props.getDtm().getLockTable();
    }

    public List<DtmPendingRow> buscarPendentesOrdenado(int limit) {
        String sql =
            "SELECT v.\"DTM\" AS id_dtm, " +
            "       v.json_pedidocoleta::text AS json_payload, " +
            "       v.prioridade_ordem AS prioridade " +
            "  FROM " + dtmView + " v " +
            "  LEFT JOIN " + lockTable + " l " +
            "    ON l.id_dtm = v.\"DTM\" " +
            " WHERE (l.id_dtm IS NULL) " +
            "    OR (COALESCE(l.processed, false) = false AND l.coleta_gerada IS NULL)" +
            "    OR (l.coleta_gerada IS NOT NULL AND COALESCE(l.processed, false) = false) " +
            "   AND COALESCE(l.processing, false) = false" +
            " ORDER BY " +
            "          CASE WHEN l.coleta_gerada IS NOT NULL AND COALESCE(l.processed, false) = FALSE THEN 0 ELSE v.prioridade_ordem END NULLS LAST, " +
            "          v.\"Hora Coleta\" ASC, " +
            "          v.\"DTM\" ASC " +
            " LIMIT ?";

        return jdbc.query(sql, ps -> ps.setInt(1, limit), (rs, rowNum) -> {
            DtmPendingRow r = new DtmPendingRow();
            r.setIdDtm(rs.getLong("id_dtm"));
            r.setJsonPedidoColeta(rs.getString("json_payload"));
            int p = rs.getInt("prioridade");
            r.setPrioridade(rs.wasNull() ? null : p);
            return r;
        });
    }
}