package com.tecnolog.autocoleta.salvarcoleta;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public class AddOcorrenciaRequest {
    @JsonProperty("idDtm")
    private long idDtm;

    @JsonProperty("idOcorrencia")
    private int idOcorrencia;

    @JsonProperty("dsObservacoes")
    private String dsObservacoes;

    @JsonProperty("dtOcorrencia")
    private OffsetDateTime dtOcorrencia;

    @JsonProperty("AccessToken")
    private String accessToken;

    public long getIdDtm() { return idDtm; }
    public void setIdDtm(long idDtm) { this.idDtm = idDtm; }
    public int getIdOcorrencia() { return idOcorrencia; }
    public void setIdOcorrencia(int idOcorrencia) { this.idOcorrencia = idOcorrencia; }
    public String getDsObservacoes() { return dsObservacoes; }
    public void setDsObservacoes(String dsObservacoes) { this.dsObservacoes = dsObservacoes; }
    public OffsetDateTime getDtOcorrencia() { return dtOcorrencia; }
    public void setDtOcorrencia(OffsetDateTime dtOcorrencia) { this.dtOcorrencia = dtOcorrencia; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}