package com.tecnolog.autocoleta.salvarcoleta.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SalvaColetaDimensoesModel {

    @JsonProperty("id")   private Integer id;
    @JsonProperty("comp") private Double comp;
    @JsonProperty("larg") private Double larg;
    @JsonProperty("alt")  private Double alt;
    @JsonProperty("qt")   private Integer qt;
    @JsonProperty("kg")   private Double kg;

    public Integer getId() { return id; }   public void setId(Integer v) { id = v; }
    public Double getComp() { return comp; }public void setComp(Double v) { comp = v; }
    public Double getLarg() { return larg; }public void setLarg(Double v) { larg = v; }
    public Double getAlt() { return alt; }  public void setAlt(Double v) { alt = v; }
    public Integer getQt() { return qt; }   public void setQt(Integer v) { qt = v; }
    public Double getKg() { return kg; }    public void setKg(Double v) { kg = v; }
}
