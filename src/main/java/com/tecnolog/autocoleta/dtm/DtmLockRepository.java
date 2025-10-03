package com.tecnolog.autocoleta.dtm;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.tecnolog.autocoleta.config.AppProperties;

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
    String idx = "dtm_automation_lock_processed_idx"; 

    String createSql =
        "CREATE TABLE IF NOT EXISTS " + tbl + " (" +
        "  id_dtm BIGINT PRIMARY KEY, " +
        "  processed BOOLEAN NOT NULL DEFAULT FALSE, " +
        "  processed_at TIMESTAMP WITH TIME ZONE NULL, " +
        "  last_error TEXT NULL" +
        ")";
    jdbc.execute(createSql);

    String idxSql =
        "CREATE INDEX IF NOT EXISTS " + idx +
        " ON " + tbl + " (processed)";
    jdbc.execute(idxSql);
}

    public boolean tryLock(long idDtm) {
        String tbl = props.getDtm().getLockTable();

        String insertIfNotExists =
            "INSERT INTO " + tbl + " (id_dtm, processed) " +
            "SELECT ?, FALSE " +
            "WHERE NOT EXISTS (SELECT 1 FROM " + tbl + " WHERE id_dtm = ?)";
        jdbc.update(insertIfNotExists, idDtm, idDtm);

        Boolean processed = jdbc.queryForObject(
            "SELECT processed FROM " + tbl + " WHERE id_dtm = ?",
            new Object[]{idDtm},
            Boolean.class
        );
        return processed != null && !processed;
    }

    public void markProcessed(long idDtm) {
        String sql = "UPDATE " + props.getDtm().getLockTable() +
                     " SET processed = TRUE, processed_at = now(), last_error = NULL " +
                     "WHERE id_dtm = ?";
        int n = jdbc.update(sql, idDtm);
        if (n == 0) {
            String ins = "INSERT INTO " + props.getDtm().getLockTable() +
                         " (id_dtm, processed, processed_at) VALUES (?, TRUE, now())";
            jdbc.update(ins, idDtm);
        }
    }

    public void markError(long idDtm, String msg) {
        String sql = "UPDATE " + props.getDtm().getLockTable() +
                     " SET processed = FALSE, last_error = ? " +
                     "WHERE id_dtm = ?";
        int n = jdbc.update(sql, msg, idDtm);
        if (n == 0) {
            String ins = "INSERT INTO " + props.getDtm().getLockTable() +
                         " (id_dtm, processed, processed_at, last_error) VALUES (?, FALSE, NULL, ?)";
            jdbc.update(ins, idDtm, msg);
        }
    }
}
