package com.tecnolog.autocoleta.dto;

import java.util.List;

/**
 * Payload que a automação envia para o endpoint /api/v1/PedidoColeta/SalvaColeta
 */
public class SalvarColetaRequest {

    private String tokenHash;
    private Long idPedidoColeta;
    private Long idCotacao;
    private Long idDtm;
    private Long idRemetente;
    private Long idDestinatario;
    private Long idTomador;
    private Long idLocalColeta;

    private String dtColeta;
    private String dtEntrega;
    private String hrColetaInicio;
    private String hrColetaFim;

    private Integer idTipoColeta;
    private Integer tpModal;
    private Integer idFilialResposavel;   // atenção: no legado está "Resposavel" mesmo
    private Integer idAgente;

    private String cdEnderecoCEP;
    private String dsEndereco;
    private String nrEnderecoNR;
    private String dsEnderecoBairro;
    private String dsEnderecoComplento;   // typo mantido p/ compatibilidade
    private Integer idEnderecoCidade;

    private String nrTelefone;
    private String dsSolicitante;
    private String dsProcurarPor;
    private String nrReferencia;
    private String nrPedidoCliente;

    private Integer idNaturezaCarga;
    private Integer idEmbalagem;

    private List<NotaFiscalDto> NF;
    private List<DimensaoDto> Dimensoes;
    private List<MonitoramentoDto> Monitoramento;

    private String dsComentarios;

    // === Getters/Setters ===

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Long getIdPedidoColeta() { return idPedidoColeta; }
    public void setIdPedidoColeta(Long idPedidoColeta) { this.idPedidoColeta = idPedidoColeta; }

    public Long getIdCotacao() { return idCotacao; }
    public void setIdCotacao(Long idCotacao) { this.idCotacao = idCotacao; }

    public Long getIdDtm() { return idDtm; }
    public void setIdDtm(Long idDtm) { this.idDtm = idDtm; }

    public Long getIdRemetente() { return idRemetente; }
    public void setIdRemetente(Long idRemetente) { this.idRemetente = idRemetente; }

    public Long getIdDestinatario() { return idDestinatario; }
    public void setIdDestinatario(Long idDestinatario) { this.idDestinatario = idDestinatario; }

    public Long getIdTomador() { return idTomador; }
    public void setIdTomador(Long idTomador) { this.idTomador = idTomador; }

    public Long getIdLocalColeta() { return idLocalColeta; }
    public void setIdLocalColeta(Long idLocalColeta) { this.idLocalColeta = idLocalColeta; }

    public String getDtColeta() { return dtColeta; }
    public void setDtColeta(String dtColeta) { this.dtColeta = dtColeta; }

    public String getDtEntrega() { return dtEntrega; }
    public void setDtEntrega(String dtEntrega) { this.dtEntrega = dtEntrega; }

    public String getHrColetaInicio() { return hrColetaInicio; }
    public void setHrColetaInicio(String hrColetaInicio) { this.hrColetaInicio = hrColetaInicio; }

    public String getHrColetaFim() { return hrColetaFim; }
    public void setHrColetaFim(String hrColetaFim) { this.hrColetaFim = hrColetaFim; }

    public Integer getIdTipoColeta() { return idTipoColeta; }
    public void setIdTipoColeta(Integer idTipoColeta) { this.idTipoColeta = idTipoColeta; }

    public Integer getTpModal() { return tpModal; }
    public void setTpModal(Integer tpModal) { this.tpModal = tpModal; }

    public Integer getIdFilialResposavel() { return idFilialResposavel; }
    public void setIdFilialResposavel(Integer idFilialResposavel) { this.idFilialResposavel = idFilialResposavel; }

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

    public List<NotaFiscalDto> getNF() { return NF; }
    public void setNF(List<NotaFiscalDto> NF) { this.NF = NF; }

    public List<DimensaoDto> getDimensoes() { return Dimensoes; }
    public void setDimensoes(List<DimensaoDto> dimensoes) { Dimensoes = dimensoes; }

    public List<MonitoramentoDto> getMonitoramento() { return Monitoramento; }
    public void setMonitoramento(List<MonitoramentoDto> monitoramento) { Monitoramento = monitoramento; }

    public String getDsComentarios() { return dsComentarios; }
    public void setDsComentarios(String dsComentarios) { this.dsComentarios = dsComentarios; }

    // === Classes internas ===
    public static class NotaFiscalDto {
        private Long id;
        private String nrChave;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getNrChave() { return nrChave; }
        public void setNrChave(String nrChave) { this.nrChave = nrChave; }
    }

    public static class DimensaoDto {
        private double comp;
        private double larg;
        private double alt;
        private int qt;
        private double kg;

        public double getComp() { return comp; }
        public void setComp(double comp) { this.comp = comp; }

        public double getLarg() { return larg; }
        public void setLarg(double larg) { this.larg = larg; }

        public double getAlt() { return alt; }
        public void setAlt(double alt) { this.alt = alt; }

        public int getQt() { return qt; }
        public void setQt(int qt) { this.qt = qt; }

        public double getKg() { return kg; }
        public void setKg(double kg) { this.kg = kg; }
    }

    public static class MonitoramentoDto {
        private Long id;
        private String nome;
        private String telefone;
        private String email;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }

        public String getTelefone() { return telefone; }
        public void setTelefone(String telefone) { this.telefone = telefone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
