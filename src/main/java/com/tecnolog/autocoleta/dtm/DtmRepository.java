// DtmRepository.java (AJUSTADO)
package com.tecnolog.autocoleta.dtm;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DtmRepository {

    private final JdbcTemplate jdbc;

    public DtmRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<DtmPendingRow> buscarPendentesOrdenado(int limit) {
        String sql =
            "SELECT v.\"DTM\" AS id_dtm, " +
            "       v.json_pedidocoleta::text AS json_payload, " +
            "       v.prioridade_ordem AS prioridade " +
            "  FROM public.vw_dtm_pedidocoleta_unica v " +
            "  LEFT JOIN public.dtm_automation_lock l " +
            "    ON l.id_dtm = v.\"DTM\" " +
            " WHERE (l.locked_at IS NULL AND COALESCE(l.processing, false) = false AND COALESCE(l.processed, false) = false) " +
            "    OR (l.coleta_gerada IS NOT NULL AND COALESCE(l.processed, false) = false AND COALESCE(l.processing, false) = false) " + 
            " ORDER BY " +
            "          CASE WHEN l.coleta_gerada IS NOT NULL AND COALESCE(l.processed, false) = FALSE THEN 0 ELSE v.prioridade_ordem END NULLS LAST, " + 
            "          v.\"Hora Coleta\" ASC, " +
            "          v.\"DTM\" ASC " +
            " LIMIT ?";

        return jdbc.query(sql, ps -> ps.setInt(1, limit), (rs, i) -> {
            DtmPendingRow r = new DtmPendingRow();
            r.setIdDtm(rs.getLong("id_dtm"));
            r.setJsonPedidoColeta(rs.getString("json_payload"));
            int p = rs.getInt("prioridade");
            r.setPrioridade(rs.wasNull() ? null : p);
            return r;
        });
    }
}