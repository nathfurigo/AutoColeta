package com.tecnolog.autocoleta.dto.salvarcoleta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalvarColetaResponse {

    @JsonProperty("erro") 
    private boolean erro;

    @JsonProperty("response") 
    private String response;

    public boolean isErro() { return erro; }
    public void setErro(boolean erro) { this.erro = erro; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
}