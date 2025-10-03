package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DtmLockRepository {
    private final JdbcTemplate jdbc;
    private final AppProperties props;

    public DtmLockRepository(JdbcTemplate jdbc, AppProperties props) {
        this.jdbc = jdbc;
        this.props = props;
        ensureTable();
    }

    private void ensureTable() {
        String tbl = props.getDtm().getLockTable();
        String createSql =
            "CREATE TABLE IF NOT EXISTS " + tbl + " (" +
            "id_dtm BIGINT PRIMARY KEY, " +
            "processed BOOLEAN NOT NULL DEFAULT FALSE, " +
            "processed_at TIMESTAMPTZ NULL, " +
            "last_error TEXT NULL" +
            ")";
        jdbc.execute(createSql);

        // índice auxiliar
        String idxSql = "CREATE INDEX IF NOT EXISTS idx_" + tbl + "_processed ON " + tbl + " (processed)";
        jdbc.execute(idxSql);
    }

    public void markProcessed(long idDtm) {
        String sql = String.format(
            "INSERT INTO %s (id_dtm, processed, processed_at, last_error) " +
            "VALUES (?, TRUE, NOW(), NULL) " +
            "ON CONFLICT (id_dtm) DO UPDATE " +
            "SET processed = EXCLUDED.processed, processed_at = EXCLUDED.processed_at, last_error = NULL",
            props.getDtm().getLockTable()
        );
        jdbc.update(sql, idDtm);
    }

    public void markError(long idDtm, String msg) {
        String sql = String.format(
            "INSERT INTO %s (id_dtm, processed, processed_at, last_error) " +
            "VALUES (?, FALSE, NULL, ?) " +
            "ON CONFLICT (id_dtm) DO UPDATE " +
            "SET processed = FALSE, last_error = EXCLUDED.last_error",
            props.getDtm().getLockTable()
        );
        jdbc.update(sql, idDtm, msg);
    }
}
