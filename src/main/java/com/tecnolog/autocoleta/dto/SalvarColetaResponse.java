package com.tecnolog.autocoleta.dto;

/**
 * Representa a resposta do endpoint:
 *   POST /api/v1/PedidoColeta/SalvaColeta
 *
 * O campo "response" pode conter tanto o ID do pedido criado (número)
 * quanto uma mensagem de erro ou retorno genérico (string).
 */
public class SalvarColetaResponse {

    private boolean erro;
    private Object response; // pode ser Integer, String ou outro tipo

    /** Construtor padrão necessário para desserialização JSON */
    public SalvarColetaResponse() {
    }

    public boolean isErro() {
        return erro;
    }

    public void setErro(boolean erro) {
        this.erro = erro;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    @Override
    public String toString() {
        String respStr = (response != null ? response.toString() : "null");
        return "SalvarColetaResponse{" +
                "erro=" + erro +
                ", response=" + respStr +
                '}';
    }
}
