package com.tecnolog.autocoleta.dtm;

import java.time.OffsetDateTime;

public class DtmLockStatus {
    
    public OffsetDateTime lockedAt;
    public boolean processing;
    public boolean processed;
    public String coletaGerada;
    
    public DtmLockStatus() { }

    public OffsetDateTime getLockedAt() { return lockedAt; }
    public boolean isProcessing() { return processing; }
    public boolean isProcessed() { return processed; }
    public String getColetaGerada() { return coletaGerada; }

    public void setLockedAt(OffsetDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public void setProcessing(boolean processing) {
        this.processing = processing;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public void setColetaGerada(String coletaGerada) {
        this.coletaGerada = coletaGerada;
    }

}