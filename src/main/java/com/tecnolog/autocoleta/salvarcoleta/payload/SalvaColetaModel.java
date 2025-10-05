package com.tecnolog.autocoleta.salvarcoleta.payload;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class SalvaColetaModel {

    private String tokenHash;

    private int idPedidoColeta;
    private int idCotacao;

    @NotNull
    private Integer idRemetente;
    @NotNull
    private Integer idDestinatario;
    @NotNull
    private Long idDtm;
    @NotNull
    private Integer idTomador;
    @NotNull
    private Integer idLocalColeta;

    @NotNull
    private LocalDate dtColeta;
    @NotNull
    private LocalDate dtEntrega;
    @NotBlank
    private String hrColetaInicio;
    @NotBlank
    private String hrColetaFim;

    @NotNull
    private Integer idTipoColeta;

    @NotNull
    private Integer tpModal;

    @NotNull
    private Integer idFilialResponsavel;
    private Integer idAgente;

    @NotBlank
    private String cdEnderecoCEP;
    @NotBlank
    private String dsEndereco;
    @NotBlank
    private String nrEnderecoNR;
    @NotBlank
    private String dsEnderecoBairro;
    private String dsEnderecoComplento;
    @NotNull
    private Integer idEnderecoCidade;

    private String nrTelefone;
    private String dsSolicitante;
    private String dsProcurarPor;
    private String nrReferencia;
    private String nrPedidoCliente;
    private Integer idNaturezaCarga;
    private Integer idEmbalagem;

    private List<SalvaColetaNFModel> NF;
    private List<SalvaColetaDimensoesModel> Dimensoes;
    private List<SalvaColetaMonitoramentoModel> Monitoramento;
    private String dsComentarios;

    // ===== Getters e Setters =====

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public int getIdPedidoColeta() { return idPedidoColeta; }
    public void setIdPedidoColeta(int idPedidoColeta) { this.idPedidoColeta = idPedidoColeta; }

    public int getIdCotacao() { return idCotacao; }
    public void setIdCotacao(int idCotacao) { this.idCotacao = idCotacao; }

    public Integer getIdRemetente() { return idRemetente; }
    public void setIdRemetente(Integer idRemetente) { this.idRemetente = idRemetente; }

    public Integer getIdDestinatario() { return idDestinatario; }
    public void setIdDestinatario(Integer idDestinatario) { this.idDestinatario = idDestinatario; }

    public Long getIdDtm() { return idDtm; }
    public void setIdDtm(Long idDtm) { this.idDtm = idDtm; }

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
    public void setIdFilialResponsavel(Integer idFilialResposavel) { this.idFilialResponsavel = idFilialResposavel; }

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

    public String getDsEnderecoComplento() { return dsEnderecoComplento; }
    public void setDsEnderecoComplento(String dsEnderecoComplento) { this.dsEnderecoComplento = dsEnderecoComplento; }

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
    public void setDimensoes(List<SalvaColetaDimensoesModel> Dimensoes) { this.Dimensoes = Dimensoes; }

    public List<SalvaColetaMonitoramentoModel> getMonitoramento() { return Monitoramento; }
    public void setMonitoramento(List<SalvaColetaMonitoramentoModel> Monitoramento) { this.Monitoramento = Monitoramento; }

    public String getDsComentarios() { return dsComentarios; }
    public void setDsComentarios(String dsComentarios) { this.dsComentarios = dsComentarios; }
}
