package com.tecnolog.autocoleta.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SalvarColetaRequest {

  @JsonProperty("Id")                   private String id;
  @JsonProperty("Dtm")                  private Long dtm;
  @JsonProperty("Versao")               private Integer versao;
  @JsonProperty("DtInicio")             private String dtInicio;   // "YYYY-MM-DDTHH:mm:ss"
  @JsonProperty("DtFim")                private String dtFim;
  @JsonProperty("NivelServico")         private String nivelServico;
  @JsonProperty("TipoServico")          private String tipoServico;
  @JsonProperty("Transportadora")       private String transportadora;
  @JsonProperty("Analista")             private String analista;
  @JsonProperty("Solicitante")          private String solicitante;
  @JsonProperty("Referencia")           private String referencia;
  @JsonProperty("Observacoes")          private String observacoes;
  @JsonProperty("ValorTotalComImposto") private Double valorTotalComImposto;

  @JsonProperty("Origem")               private Origem origem;
  @JsonProperty("Destino")              private Destino destino;
  @JsonProperty("NotasFiscaisRelacionadas") private List<NotaFiscal> notasFiscaisRelacionadas;
  @JsonProperty("Cargas")               private List<Carga> cargas;

  @JsonProperty("TokenHash")            private String tokenHash;

  // GET/SET
  public String getId(){ return id; } public void setId(String v){ id=v; }
  public Long getDtm(){ return dtm; } public void setDtm(Long v){ dtm=v; }
  public Integer getVersao(){ return versao; } public void setVersao(Integer v){ versao=v; }
  public String getDtInicio(){ return dtInicio; } public void setDtInicio(String v){ dtInicio=v; }
  public String getDtFim(){ return dtFim; } public void setDtFim(String v){ dtFim=v; }
  public String getNivelServico(){ return nivelServico; } public void setNivelServico(String v){ nivelServico=v; }
  public String getTipoServico(){ return tipoServico; } public void setTipoServico(String v){ tipoServico=v; }
  public String getTransportadora(){ return transportadora; } public void setTransportadora(String v){ transportadora=v; }
  public String getAnalista(){ return analista; } public void setAnalista(String v){ analista=v; }
  public String getSolicitante(){ return solicitante; } public void setSolicitante(String v){ solicitante=v; }
  public String getReferencia(){ return referencia; } public void setReferencia(String v){ referencia=v; }
  public String getObservacoes(){ return observacoes; } public void setObservacoes(String v){ observacoes=v; }
  public Double getValorTotalComImposto(){ return valorTotalComImposto; } public void setValorTotalComImposto(Double v){ valorTotalComImposto=v; }
  public Origem getOrigem(){ return origem; } public void setOrigem(Origem v){ origem=v; }
  public Destino getDestino(){ return destino; } public void setDestino(Destino v){ destino=v; }
  public List<NotaFiscal> getNotasFiscaisRelacionadas(){ return notasFiscaisRelacionadas; } public void setNotasFiscaisRelacionadas(List<NotaFiscal> v){ notasFiscaisRelacionadas=v; }
  public List<Carga> getCargas(){ return cargas; } public void setCargas(List<Carga> v){ cargas=v; }
  public String getTokenHash(){ return tokenHash; } public void setTokenHash(String v){ tokenHash=v; }

  // Tipos aninhados
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Origem {
    @JsonProperty("Nome") private String nome;
    @JsonProperty("Cep")  private String cep;
    @JsonProperty("Endereco") private String endereco;
    @JsonProperty("Numero")   private String numero;
    @JsonProperty("Bairro")   private String bairro;
    @JsonProperty("Complemento") private String complemento;
    @JsonProperty("Cidade")   private String cidade;
    @JsonProperty("UF")       private String uf;
    @JsonProperty("ContatosOrigem") private List<Contato> contatosOrigem;

    public String getNome(){return nome;} public void setNome(String v){nome=v;}
    public String getCep(){return cep;} public void setCep(String v){cep=v;}
    public String getEndereco(){return endereco;} public void setEndereco(String v){endereco=v;}
    public String getNumero(){return numero;} public void setNumero(String v){numero=v;}
    public String getBairro(){return bairro;} public void setBairro(String v){bairro=v;}
    public String getComplemento(){return complemento;} public void setComplemento(String v){complemento=v;}
    public String getCidade(){return cidade;} public void setCidade(String v){cidade=v;}
    public String getUf(){return uf;} public void setUf(String v){uf=v;}
    public List<Contato> getContatosOrigem(){return contatosOrigem;} public void setContatosOrigem(List<Contato> v){contatosOrigem=v;}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Destino {
    @JsonProperty("Nome") private String nome;
    @JsonProperty("Cep")  private String cep;
    @JsonProperty("Endereco") private String endereco;
    @JsonProperty("Numero")   private String numero;
    @JsonProperty("Bairro")   private String bairro;
    @JsonProperty("Complemento") private String complemento;
    @JsonProperty("Cidade")   private String cidade;
    @JsonProperty("UF")       private String uf;
    @JsonProperty("ContatosDestino") private List<Contato> contatosDestino;

    public String getNome(){return nome;} public void setNome(String v){nome=v;}
    public String getCep(){return cep;} public void setCep(String v){cep=v;}
    public String getEndereco(){return endereco;} public void setEndereco(String v){endereco=v;}
    public String getNumero(){return numero;} public void setNumero(String v){numero=v;}
    public String getBairro(){return bairro;} public void setBairro(String v){bairro=v;}
    public String getComplemento(){return complemento;} public void setComplemento(String v){complemento=v;}
    public String getCidade(){return cidade;} public void setCidade(String v){cidade=v;}
    public String getUf(){return uf;} public void setUf(String v){uf=v;}
    public List<Contato> getContatosDestino(){return contatosDestino;} public void setContatosDestino(List<Contato> v){contatosDestino=v;}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Contato {
    @JsonProperty("Nome")     private String nome;
    @JsonProperty("Telefone") private String telefone;
    @JsonProperty("Email")    private String email;

    public String getNome(){return nome;} public void setNome(String v){nome=v;}
    public String getTelefone(){return telefone;} public void setTelefone(String v){telefone=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class NotaFiscal {
    @JsonProperty("Numero")   private String numero;
    @JsonProperty("Serie")    private String serie;
    @JsonProperty("Subserie") private String subserie;
    @JsonProperty("Valor")    private Double valor;

    public String getNumero(){return numero;} public void setNumero(String v){numero=v;}
    public String getSerie(){return serie;} public void setSerie(String v){serie=v;}
    public String getSubserie(){return subserie;} public void setSubserie(String v){subserie=v;}
    public Double getValor(){return valor;} public void setValor(Double v){valor=v;}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Carga {
    @JsonProperty("Descricao")     private String descricao;
    @JsonProperty("Embalagem")     private String embalagem;
    @JsonProperty("Quantidade")    private Integer quantidade;
    @JsonProperty("Comp")          private Double comp;
    @JsonProperty("Larg")          private Double larg;
    @JsonProperty("Alt")           private Double alt;
    @JsonProperty("PesoBruto")     private Double pesoBruto;
    @JsonProperty("PesoCubado")    private Double pesoCubado;
    @JsonProperty("PesoTaxado")    private Double pesoTaxado;
    @JsonProperty("ValorMaterial") private Double valorMaterial;

    public String getDescricao(){return descricao;} public void setDescricao(String v){descricao=v;}
    public String getEmbalagem(){return embalagem;} public void setEmbalagem(String v){embalagem=v;}
    public Integer getQuantidade(){return quantidade;} public void setQuantidade(Integer v){quantidade=v;}
    public Double getComp(){return comp;} public void setComp(Double v){comp=v;}
    public Double getLarg(){return larg;} public void setLarg(Double v){larg=v;}
    public Double getAlt(){return alt;} public void setAlt(Double v){alt=v;}
    public Double getPesoBruto(){return pesoBruto;} public void setPesoBruto(Double v){pesoBruto=v;}
    public Double getPesoCubado(){return pesoCubado;} public void setPesoCubado(Double v){pesoCubado=v;}
    public Double getPesoTaxado(){return pesoTaxado;} public void setPesoTaxado(Double v){pesoTaxado=v;}
    public Double getValorMaterial(){return valorMaterial;} public void setValorMaterial(Double v){valorMaterial=v;}
  }
}
