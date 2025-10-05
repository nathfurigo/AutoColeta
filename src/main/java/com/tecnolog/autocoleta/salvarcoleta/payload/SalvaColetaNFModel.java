package com.tecnolog.autocoleta.salvarcoleta.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SalvaColetaNFModel {

    @JsonProperty("Id")     private Integer id;
    @JsonProperty("Chave")  private String chave;
    @JsonProperty("Numero") private Integer numero;   // Integer
    @JsonProperty("Serie")  private Integer serie;    // Integer
    @JsonProperty("Valor")  private Double valor;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public Integer getSerie() { return serie; }
    public void setSerie(Integer serie) { this.serie = serie; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
}
