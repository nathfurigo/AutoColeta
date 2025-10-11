package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class DtmLockRepository {
    private static final Logger log = LoggerFactory.getLogger(DtmLockRepository.class);

    private final JdbcTemplate jdbc;
    private final String lockTable;

    public DtmLockRepository(@Qualifier("postgresJdbcTemplate") JdbcTemplate jdbc, AppProperties props) {
        this.jdbc = jdbc;
        this.lockTable = props.getDtm().getLockTable();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            ensureTableAndColumns();
        } catch (Exception e) {
            log.error("Falha ao garantir a estrutura da tabela de lock '{}'", lockTable, e);
        }
    }

    private void ensureTableAndColumns() {
        String createTableSql =
            "CREATE TABLE IF NOT EXISTS " + lockTable + " (" +
            "  id_dtm BIGINT PRIMARY KEY, " +
            "  locked_at TIMESTAMPTZ NULL, " +
            "  processing BOOLEAN NOT NULL DEFAULT FALSE, " +
            "  processing_at TIMESTAMPTZ NULL, " +
            "  processed BOOLEAN NOT NULL DEFAULT FALSE, " +
            "  processed_at TIMESTAMPTZ NULL, " +
            "  last_error TEXT NULL, " +
            "  coleta_gerada VARCHAR(50) NULL" +
            ")";
        jdbc.execute(createTableSql);

        jdbc.execute("ALTER TABLE " + lockTable + " ADD COLUMN IF NOT EXISTS processing BOOLEAN NOT NULL DEFAULT FALSE");
        jdbc.execute("ALTER TABLE " + lockTable + " ADD COLUMN IF NOT EXISTS processing_at TIMESTAMPTZ NULL");
        jdbc.execute("ALTER TABLE " + lockTable + " ADD COLUMN IF NOT EXISTS coleta_gerada VARCHAR(50) NULL");

        log.info("Tabela de lock '{}' validada com sucesso.", lockTable);
    }
    
    public DtmLockStatus getLockStatus(long idDtm) {
        String sql = "SELECT locked_at, processing, processed, coleta_gerada FROM " + lockTable + " WHERE id_dtm = ?";
        
        List<DtmLockStatus> results = jdbc.query(sql, new Object[]{idDtm}, (rs, i) -> {
            DtmLockStatus status = new DtmLockStatus();
            status.lockedAt = rs.getObject("locked_at", OffsetDateTime.class);
            status.processing = rs.getBoolean("processing");
            status.processed = rs.getBoolean("processed");
            status.coletaGerada = rs.getString("coleta_gerada");
            return status;
        });

        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional("transactionManager") // Especifica o transaction manager do postgres
    public boolean tryLock(long idDtm) {
        String upsertSql = "INSERT INTO " + lockTable + " (id_dtm) VALUES (?) ON CONFLICT (id_dtm) DO NOTHING";
        jdbc.update(upsertSql, idDtm);

        String lockSql = "UPDATE " + lockTable + " " +
                         "SET processing = TRUE, processing_at = ?, locked_at = ? " +
                         "WHERE id_dtm = ? AND processing = FALSE AND processed = FALSE";
        
        int rowsAffected = jdbc.update(lockSql, OffsetDateTime.now(), OffsetDateTime.now(), idDtm);

        return rowsAffected > 0;
    }

    public void markProcessed(long idDtm, String coletaGerada) {
        String sql = "INSERT INTO " + lockTable + " (id_dtm, processing, processed, processed_at, coleta_gerada, last_error) " +
                     "VALUES (?, FALSE, TRUE, ?, ?, NULL) " +
                     "ON CONFLICT (id_dtm) DO UPDATE SET " +
                     "  processing = FALSE, " +
                     "  processed = TRUE, " +
                     "  processed_at = EXCLUDED.processed_at, " +
                     "  coleta_gerada = EXCLUDED.coleta_gerada, " +
                     "  last_error = NULL"; 

        jdbc.update(sql, idDtm, OffsetDateTime.now(), coletaGerada);
    }

    public void markError(long idDtm, String msg) {
        String sql = "INSERT INTO " + lockTable + " (id_dtm, processing, processed, last_error) " +
                     "VALUES (?, FALSE, FALSE, ?) " +
                     "ON CONFLICT (id_dtm) DO UPDATE SET " +
                     "  processing = FALSE, " + 
                     "  processed = FALSE, " +
                     "  last_error = EXCLUDED.last_error";
        
        jdbc.update(sql, idDtm, msg);
    }
}