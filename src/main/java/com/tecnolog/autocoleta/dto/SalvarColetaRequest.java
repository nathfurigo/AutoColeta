package com.tecnolog.autocoleta.dto;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
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
    private Integer idFilialResposavel;
    private Integer idAgente;

    private String cdEnderecoCEP;
    private String dsEndereco;
    private String nrEnderecoNR;
    private String dsEnderecoBairro;
    private String dsEnderecoComplento;
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

    @Data
    public class NotaFiscalDto {
        private Long id;
        private String nrChave;
    }

    @Data
    public class DimensaoDto {
        private double comp;
        private double larg;
        private double alt;
        private int qt;
        private double kg;
    }

    @Data
    public class MonitoramentoDto {
        private Long id;
        private String nome;
        private String telefone;
        private String email;
    }

}
