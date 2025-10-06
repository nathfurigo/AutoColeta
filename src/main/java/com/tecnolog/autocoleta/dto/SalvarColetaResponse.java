package com.tecnolog.autocoleta.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalvarColetaResponse {
    private boolean erro;
    private String response;

    public boolean isErro() {
        return erro;
    }
    public void setErro(boolean erro) {
        this.erro = erro;
    }

    public String getResponse() {
        return response;
    }
    public void setResponse(String response) {
        this.response = response;
    }
}
