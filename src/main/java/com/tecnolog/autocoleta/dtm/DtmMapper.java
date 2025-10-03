package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.pedidos.payload.DtmJson;

import lombok.Data;

import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@Data
public class DtmMapper {

    public DtmMapper() {}

    /**
     * Converte um DtmPendingRow, extraído do DB, para o payload DtmJson,
     * formato exigido pela API Pedidos.
     * @param r A linha DTM pendente.
     * @return O objeto DtmJson pronto para envio.
     */
    public DtmJson toPedidos(DtmPendingRow r) {
        DtmJson j = new DtmJson();

        j.Id = String.valueOf(r.getIdDtm());
        j.Dtm = r.getIdDtm();
        j.Versao = (r.getNrVersao() != null ? r.getNrVersao() : 1);
        j.DtInicio = r.getDtColetaPrev();
        j.DtFim = r.getDtEntregaPrev();
        j.NivelServico = r.getNivelServico();         
        j.TipoServico = r.getTipoServico();           
        j.Transportadora = r.getFilialResponsavel();  
        j.Analista = r.getAgente();                   
        j.Solicitante = r.getSolicitante();
        j.Referencia = nz(r.getNrReferencia(), r.getPedidoCliente());
        j.Observacoes = r.getComentarios();
        if (r.getValorTotal() != null)
            j.ValorTotalComImposto = r.getValorTotal().doubleValue();
        j.Origem = new DtmJson.Endpoint();
        j.Origem.Nome = nz(r.getOrigemNome(), r.getLocalColetaNome());
        j.Origem.Cep = r.getOrigemCep();
        j.Origem.Endereco = r.getOrigemEndereco();
        j.Origem.NrEndereco = r.getOrigemNumero();
        j.Origem.Bairro = r.getOrigemBairro();
        j.Origem.Complemento = r.getOrigemComplemento();
        j.Origem.Cidade = r.getOrigemCidade();
        j.Origem.Uf = r.getOrigemUf();
        j.Origem.ContatosOrigem = new ArrayList<>();
        DtmJson.Contato cO = new DtmJson.Contato();
        cO.Nome = r.getProcurarPor();
        cO.Telefone = r.getTelefone();
        cO.Email = r.getSolicitante();
        j.Origem.ContatosOrigem.add(cO);
        j.Destino = new DtmJson.Endpoint();
        j.Destino.Nome = r.getDestinoNome();
        j.Destino.ContatosDestino = new ArrayList<>();
        j.NotasFiscaisRelacionadas = new ArrayList<>();
        if (r.getNotas() != null) {
            for (var nf : r.getNotas()) {
                DtmJson.Nota n = new DtmJson.Nota();
                n.Numero = nf.getNumero();
                n.Serie = nf.getSerie();
                n.Subserie = nf.getSubserie();
                n.Valor = nf.getValor() != null ? nf.getValor().doubleValue() : null;
                j.NotasFiscaisRelacionadas.add(n);
            }
        }
        j.Cargas = new ArrayList<>();
        if (r.getDimensoes() != null) {
            for (var d : r.getDimensoes()) {
                DtmJson.Carga cg = new DtmJson.Carga();
                cg.Descricao = nz(d.getNatureza(), r.getNaturezaCarga());
                cg.Embalagem = nz(d.getEmbalagem(), r.getEmbalagem());
                cg.Quantidade = nzi(d.getQtd(), 1);
                cg.Comp = toMeters(d.getCompCm());
                cg.Larg = toMeters(d.getLargCm());
                cg.Alt  = toMeters(d.getAltCm());
                cg.PesoBruto  = toDbl(d.getKgBruto());
                cg.PesoCubado = toDbl(d.getKgCubado());
                cg.ValorMaterial = toDbl(d.getValor());
                j.Cargas.add(cg);
            }
        }
        if (j.Cargas.isEmpty()) {
            DtmJson.Carga cg = new DtmJson.Carga();
            cg.Descricao = nz(r.getNaturezaCarga(), "Carga");
            cg.Embalagem = nz(r.getEmbalagem(), "CAIXA");
            cg.Quantidade = 1;
            cg.Comp = 0.30;
            cg.Larg = 0.30;
            cg.Alt  = 0.30;
            cg.PesoBruto = 1.0;
            cg.PesoCubado = 1.0;
            cg.ValorMaterial = (j.ValorTotalComImposto != null ? j.ValorTotalComImposto : 0d);
            j.Cargas.add(cg);
        }
        return j;
    }

    private static String nz(String s, String def){ return (s == null || s.isBlank()) ? def : s; }
    
    private static Integer nzi(Integer v, Integer def){ return v == null ? def : v; }
    
    private static Double toMeters(Integer cm){ return cm == null ? null : (cm/100d); }
    
    private static Double toDbl(java.math.BigDecimal v){ return v == null ? null : v.doubleValue(); }
}
