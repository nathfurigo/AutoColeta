package com.tecnolog.autocoleta.dtm;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Representa uma linha de DTM (Documento de Transporte de Mercadorias)
 * pendente a ser processada, extraída da view do banco de dados.
 * * A anotação @Data do Lombok gera Getters, Setters, construtor sem argumentos, 
 * toString(), e equals()/hashCode().
 */
@Data
public class DtmPendingRow {
    private long idDtm;

    private OffsetDateTime dtColetaPrev;
    private OffsetDateTime dtEntregaPrev;
    private OffsetDateTime dtInclusao;

    private Integer nrVersao;     
    private String  nivelServico;    
    private String  tipoServico;     
    private String  filialResponsavel;
    private String  agente;
    private String  solicitante;
    private String  nrReferencia;
    private String  pedidoCliente;
    private String  comentarios;
    private BigDecimal valorTotal;

    private String localColetaNome;  
    private String procurarPor;       
    private String telefone;       

    private String origemCnpj;
    private String origemNome;
    private String origemCep;
    private String origemEndereco;
    private String origemNumero;
    private String origemComplemento;
    private String origemBairro;
    private String origemCidade;
    private String origemUf;

    private String destinoCnpj;
    private String destinoNome;
    private String destinoCep;
    private String destinoEndereco;
    private String destinoNumero;
    private String destinoComplemento;
    private String destinoBairro;
    private String destinoCidade;
    private String destinoUf;

    private String naturezaCarga;
    private String embalagem;

    private List<Nota> notas;
    private List<Dimensao> dimensoes;

    @Data
    public static class Nota {
        private Integer numero;
        private Integer serie;
        private Integer subserie;
        private BigDecimal valor;
    }

    @Data
    public static class Dimensao {
        private Integer qtd;
        private Integer compCm;
        private Integer largCm;
        private Integer altCm;
        private BigDecimal kgBruto;
        private BigDecimal kgCubado;
        private BigDecimal kgTaxado;
        private BigDecimal valor;
        private String natureza;
        private String embalagem;
    }
}
