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
        public String Numero;        
        public String Bairro;
        public String Complemento;
        public String Cidade;
        public String UF;         
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
        public Double PesoTaxado;   
        public Double ValorMaterial;
    }
    public String getId() {
      return Id;
    }
    public void setId(String id) {
      Id = id;
    }
    public Long getDtm() {
      return Dtm;
    }
    public void setDtm(Long dtm) {
      Dtm = dtm;
    }
    public Integer getVersao() {
      return Versao;
    }
    public void setVersao(Integer versao) {
      Versao = versao;
    }
    public OffsetDateTime getDtInicio() {
      return DtInicio;
    }
    public void setDtInicio(OffsetDateTime dtInicio) {
      DtInicio = dtInicio;
    }
    public OffsetDateTime getDtFim() {
      return DtFim;
    }
    public void setDtFim(OffsetDateTime dtFim) {
      DtFim = dtFim;
    }
    public String getNivelServico() {
      return NivelServico;
    }
    public void setNivelServico(String nivelServico) {
      NivelServico = nivelServico;
    }
    public String getTipoServico() {
      return TipoServico;
    }
    public void setTipoServico(String tipoServico) {
      TipoServico = tipoServico;
    }
    public String getTransportadora() {
      return Transportadora;
    }
    public void setTransportadora(String transportadora) {
      Transportadora = transportadora;
    }
    public String getAnalista() {
      return Analista;
    }
    public void setAnalista(String analista) {
      Analista = analista;
    }
    public String getSolicitante() {
      return Solicitante;
    }
    public void setSolicitante(String solicitante) {
      Solicitante = solicitante;
    }
    public String getReferencia() {
      return Referencia;
    }
    public void setReferencia(String referencia) {
      Referencia = referencia;
    }
    public String getObservacoes() {
      return Observacoes;
    }
    public void setObservacoes(String observacoes) {
      Observacoes = observacoes;
    }
    public Double getValorTotalComImposto() {
      return ValorTotalComImposto;
    }
    public void setValorTotalComImposto(Double valorTotalComImposto) {
      ValorTotalComImposto = valorTotalComImposto;
    }
    public Endpoint getOrigem() {
      return Origem;
    }
    public void setOrigem(Endpoint origem) {
      Origem = origem;
    }
    public Endpoint getDestino() {
      return Destino;
    }
    public void setDestino(Endpoint destino) {
      Destino = destino;
    }
    public List<Nota> getNotasFiscaisRelacionadas() {
      return NotasFiscaisRelacionadas;
    }
    public void setNotasFiscaisRelacionadas(List<Nota> notasFiscaisRelacionadas) {
      NotasFiscaisRelacionadas = notasFiscaisRelacionadas;
    }
    public List<Carga> getCargas() {
      return Cargas;
    }
    public void setCargas(List<Carga> cargas) {
      Cargas = cargas;
    }
}
