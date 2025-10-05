package com.tecnolog.autocoleta.salvarcoleta.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SalvaColetaMonitoramentoModel {

    @JsonProperty("Id")       private Integer id;
    @JsonProperty("Nome")     private String nome;
    @JsonProperty("Telefone") private String telefone;
    @JsonProperty("Email")    private String email;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
