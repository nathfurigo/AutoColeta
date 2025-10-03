package com.tecnolog.autocoleta.dtm;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class DtmPendingRow {
    private long idDtm;                       
    private OffsetDateTime dtColetaPrev;      
    private OffsetDateTime dtEntregaPrev;    
    private OffsetDateTime dtInclusao;        
    private String nivelServico;          

    // ORIGEM
    private String origemCnpj;
    private String origemNome;
    private String origemCep;
    private String origemEndereco;
    private String origemNumero;
    private String origemCompl;
    private String origemCidade;
    private String origemUf;
    private String origemBairro;              // caso sua view traga

    // DESTINO
    private String destinoCnpj;
    private String destinoNome;
    private String destinoCep;
    private String destinoEndereco;
    private String destinoNumero;
    private String destinoCompl;
    private String destinoCidade;
    private String destinoUf;
    private String destinoBairro;             // caso sua view traga
}
