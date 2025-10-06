package com.tecnolog.autocoleta.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalvarColetaResponse {

    @JsonProperty("erro") // Garante o mapeamento
    private boolean erro;

    @JsonProperty("response") // Garante o mapeamento
    private String response;

    // Getters e Setters devem estar presentes e corretos
    public boolean isErro() { return erro; }
    public void setErro(boolean erro) { this.erro = erro; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
}