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

    /**
     * Busca pendências na view, ignorando DTM já travadas/processadas na tabela de lock.
     */
    public List<DtmPendingRow> buscarPendentesOrdenado(int limit) {
        String sql =
            "SELECT v.\"DTM\"                  AS id_dtm, " +
            "       v.json_pedidocoleta::text AS json_payload, " +
            "       v.prioridade_ordem        AS prioridade " +
            "  FROM public.vw_dtm_pedidocoleta_unica v " +
            "  LEFT JOIN public.dtm_automation_lock l " +
            "    ON l.id_dtm = v.\"DTM\" " +
            // pega linhas SEM lock atual e não processadas
            " WHERE l.locked_at IS NULL " +
            "   AND COALESCE(l.processed, false) = false " +
            " ORDER BY v.prioridade_ordem NULLS LAST, " +
            "          v.\"Data Coleta\" ASC, " +
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
