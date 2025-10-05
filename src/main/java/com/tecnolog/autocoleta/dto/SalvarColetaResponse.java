package com.tecnolog.autocoleta.dto;

/**
 * Resposta do endpoint /api/v1/PedidoColeta/SalvaColeta
 * Convenção usada no legado:
 *   { "erro": false, "response": "12345" }
 * ou { "erro": true,  "response": "mensagem de erro" }
 */
public class SalvarColetaResponse {
    private boolean erro;
    private Object response; // pode vir número (id) ou string (mensagem)

    public SalvarColetaResponse() {}

    public boolean isErro() { return erro; }
    public void setErro(boolean erro) { this.erro = erro; }

    public Object getResponse() { return response; }
    public void setResponse(Object response) { this.response = response; }

    @Override
    public String toString() {
        return "SalvarColetaResponse{erro=" + erro + ", response=" + response + "}";
    }
}
