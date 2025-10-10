package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DtmRepository {

    private final JdbcTemplate jdbc;
    private final String viewName;
    private final String lockTable;
    private final String hrInicio;
    private final String hrFim;

    public DtmRepository(JdbcTemplate jdbc, AppProperties props) {
        this.jdbc = jdbc;
        this.viewName = props.getDtm().getView();
        this.lockTable = props.getDtm().getLockTable();
        this.hrInicio = props.getDefaults().getHrInicio(); 
        this.hrFim    = props.getDefaults().getHrFim();    
    }

    public List<DtmPendingRow> buscarPendentesParaProcessar(int limit) {
        final String sql =
            "SELECT " +
            "  v.\"DTM\"::bigint               AS id_dtm, " +
            "  v.json_pedidocoleta            AS json_payload, " +
            "  v.prioridade_ordem             AS prioridade " +
            "FROM " + viewName + " v " +
            "LEFT JOIN " + lockTable + " l " +
            "  ON l.id_dtm = v.\"DTM\" " +
            "WHERE COALESCE(l.processing, FALSE) = FALSE " +
            "  AND COALESCE(l.processed,  FALSE) = FALSE " +
            "  AND v.\"Hora Coleta\" >= ? " +   
            "  AND v.\"Hora Coleta\" <= ? " +   
            "ORDER BY " +
            "  COALESCE(v.prioridade_ordem, 9) ASC, " +
            "  v.\"Data Coleta\" ASC, " +
            "  v.\"Hora Coleta\" ASC, " +
            "  v.\"DTM\" ASC " +
            "LIMIT ?";

        return jdbc.query(sql, ps -> {
            ps.setString(1, hrInicio);
            ps.setString(2, hrFim);
            ps.setInt(3, limit);
        }, (rs, i) -> {
            DtmPendingRow r = new DtmPendingRow();
            r.setIdDtm(rs.getLong("id_dtm"));
            r.setJsonPedidoColeta(rs.getString("json_payload"));
            int p = rs.getInt("prioridade");
            r.setPrioridade(rs.wasNull() ? null : p);
            return r;
        });
    }
}
