package com.tecnolog.autocoleta.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class SalvarColetaResponse {
    private boolean erro;
    private Object response;
}
