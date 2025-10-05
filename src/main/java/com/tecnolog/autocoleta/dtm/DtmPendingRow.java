package com.tecnolog.autocoleta.dtm;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class DtmPendingRow {

    private Long idDtm;
    private Integer nrVersao;
    private OffsetDateTime dtColetaPrev;
    private OffsetDateTime dtEntregaPrev;
    private OffsetDateTime dtInclusao;

    private String nivelServico;
    private String tipoServico;
    private String filialResponsavel;
    private String agente;
    private String solicitante;
    private String nrReferencia;
    private String pedidoCliente;
    private String comentarios;
    private String localColetaNome;
    private String procurarPor;
    private String telefone;

    // Origem
    private String origemNome;
    private String origemCep;
    private String origemEndereco;
    private String origemNumero;
    private String origemBairro;
    private String origemComplemento;
    private String origemCidade;
    private String origemUf;

    // Destino
    private String destinoNome;
    private String destinoCep;
    private String destinoEndereco;
    private String destinoNumero;
    private String destinoBairro;
    private String destinoComplemento;
    private String destinoCidade;
    private String destinoUf;

    private String naturezaCarga;
    private String embalagem;
    private BigDecimal valorTotal;

    private List<Nota> notas;
    private List<Dimensao> dimensoes;

    // ======= GETTERS/SETTERS =======
    public Long getIdDtm() { return idDtm; }
    public void setIdDtm(Long idDtm) { this.idDtm = idDtm; }

    public Integer getNrVersao() { return nrVersao; }
    public void setNrVersao(Integer nrVersao) { this.nrVersao = nrVersao; }

    public OffsetDateTime getDtColetaPrev() { return dtColetaPrev; }
    public void setDtColetaPrev(OffsetDateTime dtColetaPrev) { this.dtColetaPrev = dtColetaPrev; }

    public OffsetDateTime getDtEntregaPrev() { return dtEntregaPrev; }
    public void setDtEntregaPrev(OffsetDateTime dtEntregaPrev) { this.dtEntregaPrev = dtEntregaPrev; }

    public OffsetDateTime getDtInclusao() { return dtInclusao; }
    public void setDtInclusao(OffsetDateTime dtInclusao) { this.dtInclusao = dtInclusao; }

    public String getNivelServico() { return nivelServico; }
    public void setNivelServico(String nivelServico) { this.nivelServico = nivelServico; }

    public String getTipoServico() { return tipoServico; }
    public void setTipoServico(String tipoServico) { this.tipoServico = tipoServico; }

    public String getFilialResponsavel() { return filialResponsavel; }
    public void setFilialResponsavel(String filialResponsavel) { this.filialResponsavel = filialResponsavel; }

    public String getAgente() { return agente; }
    public void setAgente(String agente) { this.agente = agente; }

    public String getSolicitante() { return solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }

    public String getNrReferencia() { return nrReferencia; }
    public void setNrReferencia(String nrReferencia) { this.nrReferencia = nrReferencia; }

    public String getPedidoCliente() { return pedidoCliente; }
    public void setPedidoCliente(String pedidoCliente) { this.pedidoCliente = pedidoCliente; }

    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }

    public String getLocalColetaNome() { return localColetaNome; }
    public void setLocalColetaNome(String localColetaNome) { this.localColetaNome = localColetaNome; }

    public String getProcurarPor() { return procurarPor; }
    public void setProcurarPor(String procurarPor) { this.procurarPor = procurarPor; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getOrigemNome() { return origemNome; }
    public void setOrigemNome(String origemNome) { this.origemNome = origemNome; }

    public String getOrigemCep() { return origemCep; }
    public void setOrigemCep(String origemCep) { this.origemCep = origemCep; }

    public String getOrigemEndereco() { return origemEndereco; }
    public void setOrigemEndereco(String origemEndereco) { this.origemEndereco = origemEndereco; }

    public String getOrigemNumero() { return origemNumero; }
    public void setOrigemNumero(String origemNumero) { this.origemNumero = origemNumero; }

    public String getOrigemBairro() { return origemBairro; }
    public void setOrigemBairro(String origemBairro) { this.origemBairro = origemBairro; }

    public String getOrigemComplemento() { return origemComplemento; }
    public void setOrigemComplemento(String origemComplemento) { this.origemComplemento = origemComplemento; }

    public String getOrigemCidade() { return origemCidade; }
    public void setOrigemCidade(String origemCidade) { this.origemCidade = origemCidade; }

    public String getOrigemUf() { return origemUf; }
    public void setOrigemUf(String origemUf) { this.origemUf = origemUf; }

    public String getDestinoNome() { return destinoNome; }
    public void setDestinoNome(String destinoNome) { this.destinoNome = destinoNome; }

    public String getDestinoCep() { return destinoCep; }
    public void setDestinoCep(String destinoCep) { this.destinoCep = destinoCep; }

    public String getDestinoEndereco() { return destinoEndereco; }
    public void setDestinoEndereco(String destinoEndereco) { this.destinoEndereco = destinoEndereco; }

    public String getDestinoNumero() { return destinoNumero; }
    public void setDestinoNumero(String destinoNumero) { this.destinoNumero = destinoNumero; }

    public String getDestinoBairro() { return destinoBairro; }
    public void setDestinoBairro(String destinoBairro) { this.destinoBairro = destinoBairro; }

    public String getDestinoComplemento() { return destinoComplemento; }
    public void setDestinoComplemento(String destinoComplemento) { this.destinoComplemento = destinoComplemento; }

    public String getDestinoCidade() { return destinoCidade; }
    public void setDestinoCidade(String destinoCidade) { this.destinoCidade = destinoCidade; }

    public String getDestinoUf() { return destinoUf; }
    public void setDestinoUf(String destinoUf) { this.destinoUf = destinoUf; }

    public String getNaturezaCarga() { return naturezaCarga; }
    public void setNaturezaCarga(String naturezaCarga) { this.naturezaCarga = naturezaCarga; }

    public String getEmbalagem() { return embalagem; }
    public void setEmbalagem(String embalagem) { this.embalagem = embalagem; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public List<Nota> getNotas() { return notas; }
    public void setNotas(List<Nota> notas) { this.notas = notas; }

    public List<Dimensao> getDimensoes() { return dimensoes; }
    public void setDimensoes(List<Dimensao> dimensoes) { this.dimensoes = dimensoes; }

    // ======== TIPOS INTERNOS ========
    public static class Nota {
        private Integer numero;
        private Integer serie;     // <- usado no mapper
        private Integer subserie;
        private BigDecimal valor;
        private String moeda;

        public Integer getNumero() { return numero; }
        public void setNumero(Integer numero) { this.numero = numero; }
        public Integer getSerie() { return serie; }
        public void setSerie(Integer serie) { this.serie = serie; }
        public Integer getSubserie() { return subserie; }
        public void setSubserie(Integer subserie) { this.subserie = subserie; }
        public BigDecimal getValor() { return valor; }
        public void setValor(BigDecimal valor) { this.valor = valor; }
        public String getMoeda() { return moeda; }
        public void setMoeda(String moeda) { this.moeda = moeda; }
    }

    public static class Dimensao {
        private Integer qtd;
        private Integer compCm;
        private Integer largCm;
        private Integer altCm;
        private BigDecimal kgBruto;
        private BigDecimal kgCubado;
        private BigDecimal kgTaxado;     // <- usado no mapper
        private BigDecimal valor;
        private String natureza;
        private String embalagem;

        public Integer getQtd() { return qtd; }
        public void setQtd(Integer qtd) { this.qtd = qtd; }
        public Integer getCompCm() { return compCm; }
        public void setCompCm(Integer compCm) { this.compCm = compCm; }
        public Integer getLargCm() { return largCm; }
        public void setLargCm(Integer largCm) { this.largCm = largCm; }
        public Integer getAltCm() { return altCm; }
        public void setAltCm(Integer altCm) { this.altCm = altCm; }
        public BigDecimal getKgBruto() { return kgBruto; }
        public void setKgBruto(BigDecimal kgBruto) { this.kgBruto = kgBruto; }
        public BigDecimal getKgCubado() { return kgCubado; }
        public void setKgCubado(BigDecimal kgCubado) { this.kgCubado = kgCubado; }
        public BigDecimal getKgTaxado() { return kgTaxado; }
        public void setKgTaxado(BigDecimal kgTaxado) { this.kgTaxado = kgTaxado; }
        public BigDecimal getValor() { return valor; }
        public void setValor(BigDecimal valor) { this.valor = valor; }
        public String getNatureza() { return natureza; }
        public void setNatureza(String natureza) { this.natureza = natureza; }
        public String getEmbalagem() { return embalagem; }
        public void setEmbalagem(String embalagem) { this.embalagem = embalagem; }
    }
}
