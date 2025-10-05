package com.tecnolog.autocoleta.salvarcoleta.payload;

public class SalvaColetaNFModel {
    private Integer Id;       // se não usar, mantenha como opcional
    private Integer Numero;   // <- o servidor costuma esperar "Numero"
    private Integer Serie;    // <- se não houver, pode deixar null
    private Double Valor;     // <- o servidor costuma esperar "Valor"

    public Integer getId() { return Id; }
    public void setId(Integer id) { Id = id; }

    public Integer getNumero() { return Numero; }
    public void setNumero(Integer numero) { Numero = numero; }

    public Integer getSerie() { return Serie; }
    public void setSerie(Integer serie) { Serie = serie; }

    public Double getValor() { return Valor; }
    public void setValor(Double valor) { Valor = valor; }
}
