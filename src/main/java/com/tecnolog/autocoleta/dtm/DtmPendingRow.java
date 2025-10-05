package com.tecnolog.autocoleta.dtm;

public class DtmPendingRow {
    private long idDtm;
    private String jsonPedidoColeta; // vem da view (coluna json_pedidocoleta)
    private Integer prioridade;      // opcional: pra ordenar

    public long getIdDtm() { return idDtm; }
    public void setIdDtm(long idDtm) { this.idDtm = idDtm; }

    public String getJsonPedidoColeta() { return jsonPedidoColeta; }
    public void setJsonPedidoColeta(String jsonPedidoColeta) { this.jsonPedidoColeta = jsonPedidoColeta; }

    public Integer getPrioridade() { return prioridade; }
    public void setPrioridade(Integer prioridade) { this.prioridade = prioridade; }
}
