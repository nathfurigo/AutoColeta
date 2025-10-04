package com.tecnolog.autocoleta.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class SalvarColetaResponse {
    private boolean erro;
    private Object response;

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
}
