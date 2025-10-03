package com.tecnolog.autocoleta.pedidos.payload;

import java.time.OffsetDateTime;
import java.util.List;

public class DtmJson {
  public String Id;
  public Long Dtm;
  public Integer Versao;

  public OffsetDateTime DtInicio;
  public OffsetDateTime DtFim;

  public String NivelServico;  
  public String TipoServico;     
  public String Transportadora;  
  public String Analista;         
  public String Solicitante;      
  public String Referencia;       
  public String Observacoes;      
  public Double ValorTotalComImposto;

  public Endpoint Origem;
  public Endpoint Destino;

  public List<Nota> NotasFiscaisRelacionadas;
  public List<Carga> Cargas;

  public static class Endpoint {
    public String Nome;
    public String Cep;
    public String Endereco;
    public String NrEndereco;
    public String Bairro;
    public String Complemento;
    public String Cidade;
    public String Uf;
    public List<Contato> ContatosOrigem;  
    public List<Contato> ContatosDestino;  
  }

  public static class Contato {
    public String Nome;
    public String Telefone;
    public String Email;
  }

  public static class Nota {
    public Integer Numero;
    public Integer Serie;
    public Integer Subserie;
    public Double Valor;
  }

  public static class Carga {
    public String Descricao;
    public String Embalagem;
    public Integer Quantidade;
    public Double Comp;       
    public Double Larg;       
    public Double Alt;        
    public Double PesoBruto;
    public Double PesoCubado;
    public Double ValorMaterial;
  }
}
