package com.tecnolog.autocoleta.salvarcoleta;

import com.tecnolog.autocoleta.config.AppProperties;
import com.tecnolog.autocoleta.dto.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.SalvarColetaResponse;
import com.tecnolog.autocoleta.dto.SyncLogOcorrenciaResponse;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
public class SalvarColetaFeignAdapter implements SalvarColetaClient {

    private final SalvarColetaFeign salvarColetaFeign;
    private final AdicionarOcorrenciaFeign adicionarOcorrenciaFeign;
    private final AuthApiClient authApiClient;
    private final String salvarColetaToken;
    private final String systemToken;

    public SalvarColetaFeignAdapter(
            SalvarColetaFeign salvarColetaFeign,
            AdicionarOcorrenciaFeign adicionarOcorrenciaFeign,
            AuthApiClient authApiClient,
            AppProperties appProperties
    ) {
        this.salvarColetaFeign = salvarColetaFeign;
        this.adicionarOcorrenciaFeign = adicionarOcorrenciaFeign;
        this.authApiClient = authApiClient;
        this.salvarColetaToken = appProperties.getSalvarColeta().getTokenHash().trim();
        this.systemToken = appProperties.getSalvarOcorrencia().getSystemToken().trim();
    }

    @Override
    public SalvarColetaResponse salvar(SalvaColetaModel body) {
        if (body.getTokenHash() == null || body.getTokenHash().isBlank()) {
            body.setTokenHash(this.salvarColetaToken);
        }
        return salvarColetaFeign.salvar(body);
    }

    @Override
    public void adicionarOcorrencia(long idDtm, String numeroColeta) {
        AuthApiClient.AuthRequest authRequest = new AuthApiClient.AuthRequest(this.systemToken);
        AuthApiClient.AuthResponse authResponse = authApiClient.getAccessToken(authRequest);

        if (authResponse == null || authResponse.isError() || authResponse.getAccessToken() == null) {
            throw new RuntimeException("Falha ao obter AccessToken para a API de Ocorrência.");
        }
        String accessToken = authResponse.getAccessToken();

        AddOcorrenciaRequest request = new AddOcorrenciaRequest();
        request.setIdDtm(idDtm);
        request.setIdOcorrencia(2);
        request.setDsObservacoes("Coleta Nº - " + numeroColeta);
        request.setDtOcorrencia(OffsetDateTime.now());
        request.setAccessToken(accessToken);

        SyncLogOcorrenciaResponse response = adicionarOcorrenciaFeign.adicionarOcorrencia(request);

        if (response != null && response.isError()) {
            throw new RuntimeException("API de Ocorrência retornou erro: " + response.getMessage());
        }
    }
}