package com.tecnolog.autocoleta.dto.salvarcoleta;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SalvaColetaMonitoramentoModel {

    @JsonProperty("nome")     private String nome;
    @JsonProperty("telefone") private String telefone;
    @JsonProperty("email")    private String email;

    public String getNome() { return nome; }           public void setNome(String v) { nome = v; }
    public String getTelefone() { return telefone; }   public void setTelefone(String v) { telefone = v; }
    public String getEmail() { return email; }         public void setEmail(String v) { email = v; }
}
