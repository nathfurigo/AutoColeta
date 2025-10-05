package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Locale;

@Repository
public class DtmLockRepository {
    private static final Logger log = LoggerFactory.getLogger(DtmLockRepository.class);

    private final JdbcTemplate jdbc;
    private final AppProperties props;

    public DtmLockRepository(JdbcTemplate jdbc, AppProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            ensureTable();
        } catch (Exception e) {
            log.error("Falha ao garantir tabela de lock '{}'", props.getDtm().getLockTable(), e);
        }
    }

    private void ensureTable() {
        String tblQualified = props.getDtm().getLockTable(); // ex: "public.dtm_automation_lock" ou "dtm_automation_lock"

        // → nomes de índice SEM schema/ponto
        String tblOnly  = tblQualified.contains(".")
                ? tblQualified.substring(tblQualified.indexOf('.') + 1)
                : tblQualified;
        String safeBase = tblOnly.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(Locale.ROOT);

        String idxProcessed = safeBase + "_processed_idx";
        String idxLocked    = safeBase + "_locked_idx";

        // 1) cria tabela (se não existir)
        String createSql =
            "CREATE TABLE IF NOT EXISTS " + tblQualified + " (" +
            "  id_dtm BIGINT PRIMARY KEY, " +
            "  processed BOOLEAN NOT NULL DEFAULT FALSE, " +
            "  processed_at TIMESTAMPTZ NULL, " +
            "  last_error TEXT NULL, " +
            "  locked_at TIMESTAMPTZ NULL" +
            ")";
        jdbc.execute(createSql);

        // 2) garante coluna locked_at (caso tabela antiga já exista sem ela)
        jdbc.execute("ALTER TABLE " + tblQualified + " ADD COLUMN IF NOT EXISTS locked_at TIMESTAMPTZ NULL");

        // 3) índices (nomes sem ‘.’)
        jdbc.execute("CREATE INDEX IF NOT EXISTS " + idxProcessed + " ON " + tblQualified + " (processed)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS " + idxLocked    + " ON " + tblQualified + " (locked_at)");

        log.info("Tabela de lock '{}' validada (índices: '{}', '{}').", tblQualified, idxProcessed, idxLocked);
    }

    public boolean tryLock(long idDtm) {
        String tbl = props.getDtm().getLockTable();

        // cria o registro se não existir
        String insertIfNotExists =
            "INSERT INTO " + tbl + " (id_dtm, processed) " +
            "SELECT ?, FALSE WHERE NOT EXISTS (SELECT 1 FROM " + tbl + " WHERE id_dtm = ?)";
        jdbc.update(insertIfNotExists, idDtm, idDtm);

        // lock simples: se ainda não processado, consideramos “travado” para este worker
        Boolean processed = jdbc.queryForObject(
            "SELECT processed FROM " + tbl + " WHERE id_dtm = ?",
            Boolean.class, idDtm
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
