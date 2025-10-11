package com.tecnolog.autocoleta.dto.salvarcoleta;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class SalvaColetaNFModel {

    @JsonProperty("id")    private Integer id;
    @JsonProperty("nr")    private String nr;
    @JsonProperty("vl")    private BigDecimal vl;
    @JsonProperty("chave") private String chave;

    public Integer getId() { return id; }      public void setId(Integer v) { id = v; }
    public String getNr() { return nr; }       public void setNr(String v) { nr = v; }
    public BigDecimal getVl() { return vl; }   public void setVl(BigDecimal v) { vl = v; }
    public String getChave() { return chave; } public void setChave(String v) { chave = v; }
}
