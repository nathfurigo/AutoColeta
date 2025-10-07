package com.tecnolog.autocoleta.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import com.tecnolog.autocoleta.salvarcoleta.payload.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalvaColetaModel {

    @JsonProperty("idDtm")            private Long idDtm;
    @JsonProperty("TokenHash")        private String tokenHash;
    @JsonProperty("idPedidoColeta")   private Integer idPedidoColeta;
    @JsonProperty("idRemetente")      private Integer idRemetente;
    @JsonProperty("idDestinatario")   private Integer idDestinatario;
    @JsonProperty("idTomador")        private Integer idTomador;
    @JsonProperty("idFilialResposavel") private Integer idFilialResposavel;
    @JsonProperty("idLocalColeta")    private Integer idLocalColeta;

    @JsonProperty("dtColeta")   @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dtColeta;

    @JsonProperty("hrColetaInicio")   private String hrColetaInicio;
    @JsonProperty("hrColetaFim")      private String hrColetaFim;

    @JsonProperty("dtEntrega")  @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dtEntrega;

    @JsonProperty("tpModal")            private Integer tpModal;
    @JsonProperty("dsEndereco")         private String dsEndereco;
    @JsonProperty("nrEnderecoNR")       private String nrEnderecoNR;
    @JsonProperty("dsEnderecoBairro")   private String dsEnderecoBairro;
    @JsonProperty("dsEnderecoComplento")private String dsEnderecoComplento;
    @JsonProperty("cdEnderecoCEP")      private String cdEnderecoCEP;
    @JsonProperty("idEnderecoCidade")   private Integer idEnderecoCidade;

    @JsonProperty("dsSolicitante")  private String dsSolicitante;
    @JsonProperty("dsProcurarPor")  private String dsProcurarPor;
    @JsonProperty("nrTelefone")     private String nrTelefone;

    @JsonProperty("idTipoColeta")   private Integer idTipoColeta;
    @JsonProperty("idAgente")       private Integer idAgente;
    @JsonProperty("idEmbalagem")    private Integer idEmbalagem;
    @JsonProperty("idNaturezaCarga")private Integer idNaturezaCarga;

    @JsonProperty("nrReferencia")   private String nrReferencia;
    @JsonProperty("nrPedidoCliente")private String nrPedidoCliente;

    @JsonProperty("NF")         private List<SalvaColetaNFModel> nf;
    @JsonProperty("Dimensoes")  private List<SalvaColetaDimensoesModel> dimensoes;
    @JsonProperty("Monitoramento") private List<SalvaColetaMonitoramentoModel> monitoramento;

    @JsonProperty("dsComentarios") private String dsComentarios;

    public Long getIdDtm() {
        return idDtm;
    }

    public void setIdDtm(Long idDtm) {
        this.idDtm = idDtm;
    }
    
    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Integer getIdPedidoColeta() {
        return idPedidoColeta;
    }

    public void setIdPedidoColeta(Integer idPedidoColeta) {
        this.idPedidoColeta = idPedidoColeta;
    }

    public Integer getIdRemetente() {
        return idRemetente;
    }

    public void setIdRemetente(Integer idRemetente) {
        this.idRemetente = idRemetente;
    }

    public Integer getIdDestinatario() {
        return idDestinatario;
    }

    public void setIdDestinatario(Integer idDestinatario) {
        this.idDestinatario = idDestinatario;
    }

    public Integer getIdTomador() {
        return idTomador;
    }

    public void setIdTomador(Integer idTomador) {
        this.idTomador = idTomador;
    }

    public Integer getIdFilialResposavel() {
        return idFilialResposavel;
    }

    public void setIdFilialResposavel(Integer idFilialResposavel) {
        this.idFilialResposavel = idFilialResposavel;
    }

    public Integer getIdLocalColeta() {
        return idLocalColeta;
    }

    public void setIdLocalColeta(Integer idLocalColeta) {
        this.idLocalColeta = idLocalColeta;
    }

    public LocalDate getDtColeta() {
        return dtColeta;
    }

    public void setDtColeta(LocalDate dtColeta) {
        this.dtColeta = dtColeta;
    }

    public String getHrColetaInicio() {
        return hrColetaInicio;
    }

    public void setHrColetaInicio(String hrColetaInicio) {
        this.hrColetaInicio = hrColetaInicio;
    }

    public String getHrColetaFim() {
        return hrColetaFim;
    }

    public void setHrColetaFim(String hrColetaFim) {
        this.hrColetaFim = hrColetaFim;
    }

    public LocalDate getDtEntrega() {
        return dtEntrega;
    }

    public void setDtEntrega(LocalDate dtEntrega) {
        this.dtEntrega = dtEntrega;
    }

    public Integer getTpModal() {
        return tpModal;
    }

    public void setTpModal(Integer tpModal) {
        this.tpModal = tpModal;
    }

    public String getDsEndereco() {
        return dsEndereco;
    }

    public void setDsEndereco(String dsEndereco) {
        this.dsEndereco = dsEndereco;
    }

    public String getNrEnderecoNR() {
        return nrEnderecoNR;
    }

    public void setNrEnderecoNR(String nrEnderecoNR) {
        this.nrEnderecoNR = nrEnderecoNR;
    }

    public String getDsEnderecoBairro() {
        return dsEnderecoBairro;
    }

    public void setDsEnderecoBairro(String dsEnderecoBairro) {
        this.dsEnderecoBairro = dsEnderecoBairro;
    }

    public String getDsEnderecoComplento() {
        return dsEnderecoComplento;
    }

    public void setDsEnderecoComplento(String dsEnderecoComplento) {
        this.dsEnderecoComplento = dsEnderecoComplento;
    }

    public String getCdEnderecoCEP() {
        return cdEnderecoCEP;
    }

    public void setCdEnderecoCEP(String cdEnderecoCEP) {
        this.cdEnderecoCEP = cdEnderecoCEP;
    }

    public Integer getIdEnderecoCidade() {
        return idEnderecoCidade;
    }

    public void setIdEnderecoCidade(Integer idEnderecoCidade) {
        this.idEnderecoCidade = idEnderecoCidade;
    }

    public String getDsSolicitante() {
        return dsSolicitante;
    }

    public void setDsSolicitante(String dsSolicitante) {
        this.dsSolicitante = dsSolicitante;
    }

    public String getDsProcurarPor() {
        return dsProcurarPor;
    }

    public void setDsProcurarPor(String dsProcurarPor) {
        this.dsProcurarPor = dsProcurarPor;
    }

    public String getNrTelefone() {
        return nrTelefone;
    }

    public void setNrTelefone(String nrTelefone) {
        this.nrTelefone = nrTelefone;
    }

    public Integer getIdTipoColeta() {
        return idTipoColeta;
    }

    public void setIdTipoColeta(Integer idTipoColeta) {
        this.idTipoColeta = idTipoColeta;
    }

    public Integer getIdAgente() {
        return idAgente;
    }

    public void setIdAgente(Integer idAgente) {
        this.idAgente = idAgente;
    }

    public Integer getIdEmbalagem() {
        return idEmbalagem;
    }

    public void setIdEmbalagem(Integer idEmbalagem) {
        this.idEmbalagem = idEmbalagem;
    }

    public Integer getIdNaturezaCarga() {
        return idNaturezaCarga;
    }

    public void setIdNaturezaCarga(Integer idNaturezaCarga) {
        this.idNaturezaCarga = idNaturezaCarga;
    }

    public String getNrReferencia() {
        return nrReferencia;
    }

    public void setNrReferencia(String nrReferencia) {
        this.nrReferencia = nrReferencia;
    }

    public String getNrPedidoCliente() {
        return nrPedidoCliente;
    }

    public void setNrPedidoCliente(String nrPedidoCliente) {
        this.nrPedidoCliente = nrPedidoCliente;
    }

    public List<SalvaColetaNFModel> getNf() {
        return nf;
    }

    public void setNf(List<SalvaColetaNFModel> nf) {
        this.nf = nf;
    }

    public List<SalvaColetaDimensoesModel> getDimensoes() {
        return dimensoes;
    }

    public void setDimensoes(List<SalvaColetaDimensoesModel> dimensoes) {
        this.dimensoes = dimensoes;
    }

    public List<SalvaColetaMonitoramentoModel> getMonitoramento() {
        return monitoramento;
    }

    public void setMonitoramento(List<SalvaColetaMonitoramentoModel> monitoramento) {
        this.monitoramento = monitoramento;
    }

    public String getDsComentarios() {
        return dsComentarios;
    }

    public void setDsComentarios(String dsComentarios) {
        this.dsComentarios = dsComentarios;
    }

        
}