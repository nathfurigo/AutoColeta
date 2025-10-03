package com.tecnolog.autocoleta.salvarcoleta.payload;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;


public class SalvaColetaModel {
    public String TokenHash; // GUID do usuário (autorização)

    public int idPedidoColeta; // 0 = inserir
    public int idCotacao; // opcional

    @NotNull
    public Integer idRemetente;
    @NotNull
    public Integer idDestinatario;
    @NotNull
    public Long idDtm; // DTM (Postgres)
    @NotNull
    public Integer idTomador;
    @NotNull
    public Integer idLocalColeta;

    @NotNull
    public LocalDate dtColeta;
    @NotNull
    public LocalDate dtEntrega;
    @NotBlank
    public String hrColetaInicio;
    @NotBlank
    public String hrColetaFim;

    @NotNull
    public Integer idTipoColeta;
    /** 0=aéreo, 1=rodoviário */
    @NotNull
    public Integer tpModal;

    @NotNull
    public Integer idFilialResposavel;
    public Integer idAgente;

    @NotBlank
    public String cdEnderecoCEP;
    @NotBlank
    public String dsEndereco;
    @NotBlank
    public String nrEnderecoNR;
    @NotBlank
    public String dsEnderecoBairro;
    public String dsEnderecoComplento;
    @NotNull
    public Integer idEnderecoCidade; // id cidade (ESL)

    public String nrTelefone;
    public String dsSolicitante;
    public String dsProcurarPor;
    public String nrReferencia;
    public String nrPedidoCliente;
    public Integer idNaturezaCarga;
    public Integer idEmbalagem;

    public List<SalvaColetaNFModel> NF;
    public List<SalvaColetaDimensoesModel> Dimensoes;
    public List<SalvaColetaMonitoramentoModel> Monitoramento;
    public String dsComentarios;
}
