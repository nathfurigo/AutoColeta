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

    /**
     * Busca DTMs pendentes diretamente da view, que contém a lógica de priorização
     * e agendamento. As DTMs que já possuem coleta gerada mas falharam no passo
     * de registrar a ocorrência são priorizadas.
     * @param limit A quantidade máxima de registros a serem retornados.
     * @return Uma lista de DTMs pendentes, ordenadas por prioridade.
     */
    public List<DtmPendingRow> buscarPendentesOrdenado(int limit) {
        String sql =
            "SELECT v.\"DTM\" AS id_dtm, " +
            "       v.json_pedidocoleta::text AS json_payload, " +
            "       v.prioridade_ordem AS prioridade " +
            "  FROM " + dtmView + " v " +
            "  LEFT JOIN " + lockTable + " l " +
            "    ON l.id_dtm = v.\"DTM\" " +
            // Condição 1: DTMs nunca tocadas (sem lock, sem processamento)
            " WHERE (l.id_dtm IS NULL) " +
            // Condição 2: DTMs que falharam, mas não criticamente (sem coleta gerada)
            "    OR (COALESCE(l.processed, false) = false AND l.coleta_gerada IS NULL)" +
            // Condição 3: DTMs que geraram coleta mas falharam ao registrar ocorrência (CRÍTICO - REPROCESSAR PRIMEIRO)
            "    OR (l.coleta_gerada IS NOT NULL AND COALESCE(l.processed, false) = false) " +
            // A view já exclui DTMs processadas, mas garantimos que não estão em processamento.
            "   AND COALESCE(l.processing, false) = false" +
            " ORDER BY " +
            // Prioridade máxima para DTMs que precisam registrar ocorrência
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