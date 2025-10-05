package com.tecnolog.autocoleta.salvarcoleta.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SalvaColetaDimensoesModel {

    @JsonProperty("Qtd")         private Integer qtd;
    @JsonProperty("Comprimento") private Double comprimento;
    @JsonProperty("Largura")     private Double largura;
    @JsonProperty("Altura")      private Double altura;
    @JsonProperty("Kg")          private Double kg;

    public Integer getQtd() { return qtd; }
    public void setQtd(Integer qtd) { this.qtd = qtd; }

    public Double getComprimento() { return comprimento; }
    public void setComprimento(Double comprimento) { this.comprimento = comprimento; }

    public Double getLargura() { return largura; }
    public void setLargura(Double largura) { this.largura = largura; }

    public Double getAltura() { return altura; }
    public void setAltura(Double altura) { this.altura = altura; }

    public Double getKg() { return kg; }
    public void setKg(Double kg) { this.kg = kg; }
}
