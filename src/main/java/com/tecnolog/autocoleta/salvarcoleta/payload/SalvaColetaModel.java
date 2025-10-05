package com.tecnolog.autocoleta.salvarcoleta.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalvaColetaModel {

    @JsonProperty("TokenHash") private String tokenHash;

    @JsonProperty("IdPedidoColeta") private Integer idPedidoColeta;
    @JsonProperty("IdCotacao")      private Integer idCotacao;

    @JsonProperty("IdDtm")          private Long idDtm;
    @JsonProperty("IdRemetente")    private Integer idRemetente;
    @JsonProperty("IdDestinatario") private Integer idDestinatario;
    @JsonProperty("IdTomador")      private Integer idTomador;
    @JsonProperty("IdLocalColeta")  private Integer idLocalColeta;

    @JsonProperty("DtColeta")       private LocalDate dtColeta;
    @JsonProperty("DtEntrega")      private LocalDate dtEntrega;
    @JsonProperty("HrColetaInicio") private String hrColetaInicio;
    @JsonProperty("HrColetaFim")    private String hrColetaFim;

    @JsonProperty("IdTipoColeta")        private Integer idTipoColeta;
    @JsonProperty("TpModal")             private Integer tpModal;
    @JsonProperty("IdFilialResponsavel") private Integer idFilialResponsavel;
    @JsonProperty("IdAgente")            private Integer idAgente;

    @JsonProperty("CdEnderecoCEP")       private String cdEnderecoCEP;
    @JsonProperty("DsEndereco")          private String dsEndereco;
    @JsonProperty("NrEnderecoNR")        private String nrEnderecoNR;
    @JsonProperty("DsEnderecoBairro")    private String dsEnderecoBairro;

    // Nome oficial esperado pelo servidor:
    @JsonProperty("DsEnderecoComplemento")
    private String dsEnderecoComplemento;

    @JsonProperty("IdEnderecoCidade") private Integer idEnderecoCidade;

    @JsonProperty("NrTelefone")     private String nrTelefone;
    @JsonProperty("DsSolicitante")  private String dsSolicitante;
    @JsonProperty("DsProcurarPor")  private String dsProcurarPor;
    @JsonProperty("NrReferencia")   private String nrReferencia;
    @JsonProperty("NrPedidoCliente") private String nrPedidoCliente;

    @JsonProperty("IdNaturezaCarga") private Integer idNaturezaCarga;
    @JsonProperty("IdEmbalagem")     private Integer idEmbalagem;

    @JsonProperty("NF")         private List<SalvaColetaNFModel> NF;
    @JsonProperty("Dimensoes")  private List<SalvaColetaDimensoesModel> Dimensoes;
    @JsonProperty("Monitoramento") private List<SalvaColetaMonitoramentoModel> Monitoramento;

    @JsonProperty("DsComentarios") private String dsComentarios;

    // Getters/Setters

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Integer getIdPedidoColeta() { return idPedidoColeta; }
    public void setIdPedidoColeta(Integer idPedidoColeta) { this.idPedidoColeta = idPedidoColeta; }

    public Integer getIdCotacao() { return idCotacao; }
    public void setIdCotacao(Integer idCotacao) { this.idCotacao = idCotacao; }

    public Long getIdDtm() { return idDtm; }
    public void setIdDtm(Long idDtm) { this.idDtm = idDtm; }

    public Integer getIdRemetente() { return idRemetente; }
    public void setIdRemetente(Integer idRemetente) { this.idRemetente = idRemetente; }

    public Integer getIdDestinatario() { return idDestinatario; }
    public void setIdDestinatario(Integer idDestinatario) { this.idDestinatario = idDestinatario; }

    public Integer getIdTomador() { return idTomador; }
    public void setIdTomador(Integer idTomador) { this.idTomador = idTomador; }

    public Integer getIdLocalColeta() { return idLocalColeta; }
    public void setIdLocalColeta(Integer idLocalColeta) { this.idLocalColeta = idLocalColeta; }

    public LocalDate getDtColeta() { return dtColeta; }
    public void setDtColeta(LocalDate dtColeta) { this.dtColeta = dtColeta; }

    public LocalDate getDtEntrega() { return dtEntrega; }
    public void setDtEntrega(LocalDate dtEntrega) { this.dtEntrega = dtEntrega; }

    public String getHrColetaInicio() { return hrColetaInicio; }
    public void setHrColetaInicio(String hrColetaInicio) { this.hrColetaInicio = hrColetaInicio; }

    public String getHrColetaFim() { return hrColetaFim; }
    public void setHrColetaFim(String hrColetaFim) { this.hrColetaFim = hrColetaFim; }

    public Integer getIdTipoColeta() { return idTipoColeta; }
    public void setIdTipoColeta(Integer idTipoColeta) { this.idTipoColeta = idTipoColeta; }

    public Integer getTpModal() { return tpModal; }
    public void setTpModal(Integer tpModal) { this.tpModal = tpModal; }

    public Integer getIdFilialResponsavel() { return idFilialResponsavel; }
    public void setIdFilialResponsavel(Integer idFilialResponsavel) { this.idFilialResponsavel = idFilialResponsavel; }

    public Integer getIdAgente() { return idAgente; }
    public void setIdAgente(Integer idAgente) { this.idAgente = idAgente; }

    public String getCdEnderecoCEP() { return cdEnderecoCEP; }
    public void setCdEnderecoCEP(String cdEnderecoCEP) { this.cdEnderecoCEP = cdEnderecoCEP; }

    public String getDsEndereco() { return dsEndereco; }
    public void setDsEndereco(String dsEndereco) { this.dsEndereco = dsEndereco; }

    public String getNrEnderecoNR() { return nrEnderecoNR; }
    public void setNrEnderecoNR(String nrEnderecoNR) { this.nrEnderecoNR = nrEnderecoNR; }

    public String getDsEnderecoBairro() { return dsEnderecoBairro; }
    public void setDsEnderecoBairro(String dsEnderecoBairro) { this.dsEnderecoBairro = dsEnderecoBairro; }

    public String getDsEnderecoComplemento() { return dsEnderecoComplemento; }
    public void setDsEnderecoComplemento(String dsEnderecoComplemento) { this.dsEnderecoComplemento = dsEnderecoComplemento; }

    // Compat com mapper antigo: "Complento"
    public String getDsEnderecoComplento() { return dsEnderecoComplemento; }
    public void setDsEnderecoComplento(String v) { this.dsEnderecoComplemento = v; }

    public Integer getIdEnderecoCidade() { return idEnderecoCidade; }
    public void setIdEnderecoCidade(Integer idEnderecoCidade) { this.idEnderecoCidade = idEnderecoCidade; }

    public String getNrTelefone() { return nrTelefone; }
    public void setNrTelefone(String nrTelefone) { this.nrTelefone = nrTelefone; }

    public String getDsSolicitante() { return dsSolicitante; }
    public void setDsSolicitante(String dsSolicitante) { this.dsSolicitante = dsSolicitante; }

    public String getDsProcurarPor() { return dsProcurarPor; }
    public void setDsProcurarPor(String dsProcurarPor) { this.dsProcurarPor = dsProcurarPor; }

    public String getNrReferencia() { return nrReferencia; }
    public void setNrReferencia(String nrReferencia) { this.nrReferencia = nrReferencia; }

    public String getNrPedidoCliente() { return nrPedidoCliente; }
    public void setNrPedidoCliente(String nrPedidoCliente) { this.nrPedidoCliente = nrPedidoCliente; }

    public Integer getIdNaturezaCarga() { return idNaturezaCarga; }
    public void setIdNaturezaCarga(Integer idNaturezaCarga) { this.idNaturezaCarga = idNaturezaCarga; }

    public Integer getIdEmbalagem() { return idEmbalagem; }
    public void setIdEmbalagem(Integer idEmbalagem) { this.idEmbalagem = idEmbalagem; }

    public List<SalvaColetaNFModel> getNF() { return NF; }
    public void setNF(List<SalvaColetaNFModel> NF) { this.NF = NF; }

    public List<SalvaColetaDimensoesModel> getDimensoes() { return Dimensoes; }
    public void setDimensoes(List<SalvaColetaDimensoesModel> dimensoes) { this.Dimensoes = dimensoes; }

    public List<SalvaColetaMonitoramentoModel> getMonitoramento() { return Monitoramento; }
    public void setMonitoramento(List<SalvaColetaMonitoramentoModel> monitoramento) { this.Monitoramento = monitoramento; }

    public String getDsComentarios() { return dsComentarios; }
    public void setDsComentarios(String dsComentarios) { this.dsComentarios = dsComentarios; }
}
