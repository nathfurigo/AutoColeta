package com.tecnolog.autocoleta.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnolog.autocoleta.config.AppProperties;
import com.tecnolog.autocoleta.config.AuthApiClient;
import com.tecnolog.autocoleta.config.SalvarColetaClient;
import com.tecnolog.autocoleta.dtm.DtmLockRepository;
import com.tecnolog.autocoleta.dtm.DtmPendingRow;
import com.tecnolog.autocoleta.dtm.DtmToSalvaColetaMapper;
import com.tecnolog.autocoleta.dtm.SqlServerRepository;
import com.tecnolog.autocoleta.dto.AddOcorrenciaRequest;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvarColetaResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tecnolog.autocoleta.config.AdicionarOcorrenciaFeign;

@Service
public class DtmProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DtmProcessingService.class);

    private final SalvarColetaClient salvarColetaClient;
    private final DtmToSalvaColetaMapper dtmMapper;
    private final DtmLockRepository dtmLockRepository;
    private final ObjectMapper objectMapper;
    private final SqlServerRepository sqlServerRepository;
    private final AdicionarOcorrenciaFeign adicionarOcorrenciaFeign;
    private final AuthApiClient authApiClient;
    private final AppProperties appProperties;

    private static final int ID_OCORRENCIA_PROCESSADO = 2;

    public DtmProcessingService(
            SalvarColetaClient salvarColetaClient,
            DtmToSalvaColetaMapper dtmMapper,
            DtmLockRepository dtmLockRepository,
            ObjectMapper objectMapper,
            SqlServerRepository sqlServerRepository,
            AdicionarOcorrenciaFeign adicionarOcorrenciaFeign,
            AuthApiClient authApiClient,
            AppProperties appProperties
    ) {
        this.salvarColetaClient = salvarColetaClient;
        this.dtmMapper = dtmMapper;
        this.dtmLockRepository = dtmLockRepository;
        this.objectMapper = objectMapper;
        this.sqlServerRepository = sqlServerRepository;
        this.adicionarOcorrenciaFeign = adicionarOcorrenciaFeign;
        this.authApiClient = authApiClient;
        this.appProperties = appProperties;
    }

    public void processarDtm(DtmPendingRow dtmRow) {
        long idDtm = dtmRow.getIdDtm();

        try {
            Integer existingColetaId = sqlServerRepository.findExistingColetaIdByDtm(String.valueOf(idDtm));

            if (existingColetaId != null) {
                log.warn("DTM {} já possui uma coleta vinculada (ID: {}). A criação de uma nova coleta foi cancelada.", idDtm, existingColetaId);
                adicionarOcorrencia(idDtm, "Coleta já vinculada no TMS com o ID: " + existingColetaId);
                dtmLockRepository.markProcessed(idDtm, existingColetaId.toString());
                return; 
            }

            log.info("Nenhuma coleta existente para DTM {}. Prosseguindo com a criação.", idDtm);
            SalvaColetaModel requestPayload = dtmMapper.map(dtmRow);
            
            sqlServerRepository.preencherDadosFaltantes(requestPayload);
            
            List<String> missingFields = new ArrayList<>();
            if (requestPayload.getIdRemetente() == null) missingFields.add("idRemetente");
            if (requestPayload.getIdDestinatario() == null) missingFields.add("idDestinatario");
            if (requestPayload.getIdTomador() == null) missingFields.add("idTomador");
            if (requestPayload.getIdLocalColeta() == null) missingFields.add("idLocalColeta");
            if (requestPayload.getIdNaturezaCarga() == null) missingFields.add("idNaturezaCarga");
            if (requestPayload.getIdEmbalagem() == null) missingFields.add("idEmbalagem");
            
            if (!missingFields.isEmpty()) {
                throw new IllegalStateException("Falha de enriquecimento de dados. IDs ausentes: " + String.join(", ", missingFields));
            }

            SalvarColetaResponse response = salvarColetaClient.salvar(requestPayload);
            log.info("Resposta da API SalvarColeta para DTM {}: erro={}, response='{}'", idDtm, response.isErro(), response.getResponse());

            if (response == null || response.isErro()) {
                throw new IllegalStateException("API SalvarColeta retornou erro: " + (response != null ? response.getResponse() : "Resposta nula"));
            }

            String numeroColetaGerada = response.getResponse();
            log.info("DTM {} - Coleta {} gerada com sucesso. Registrando ocorrência...", idDtm, numeroColetaGerada);
            adicionarOcorrencia(idDtm, "Coleta Nº - " + numeroColetaGerada);
            dtmLockRepository.markProcessed(idDtm, numeroColetaGerada);
            log.info("Ocorrência para DTM {} (Coleta {}) registrada com sucesso.", idDtm, numeroColetaGerada);

        } catch (Exception e) {
            String errorMessage = "Falha ao processar DTM " + idDtm + ". Causa: " + e.getMessage();
            log.error(errorMessage);
            dtmLockRepository.markError(idDtm, errorMessage);
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    private void adicionarOcorrencia(long dtmId, String observacoes) {
        try {
            String systemToken = appProperties.getSalvarOcorrencia().getSystemToken();
            AuthApiClient.AuthRequest authRequest = new AuthApiClient.AuthRequest(systemToken);
            AuthApiClient.AuthResponse authResponse = authApiClient.getAccessToken(authRequest);

            if (authResponse == null || authResponse.isError() || authResponse.getAccessToken() == null) {
                throw new RuntimeException("Falha ao obter AccessToken para a API de Ocorrência.");
            }
            String accessToken = authResponse.getAccessToken();
            log.debug("AccessToken para DTM {} obtido com sucesso.", dtmId);

            AddOcorrenciaRequest requestBody = new AddOcorrenciaRequest();
            requestBody.setIdDtm(dtmId);
            requestBody.setIdOcorrencia(ID_OCORRENCIA_PROCESSADO);
            requestBody.setDsObservacoes(observacoes);
            requestBody.setDtOcorrencia(OffsetDateTime.now());
            requestBody.setAccessToken(accessToken);

            this.adicionarOcorrenciaFeign.adicionarOcorrencia(requestBody);

        } catch (Exception e) {
            log.error("Falha ao enviar ocorrência via Feign para a DTM {}. Causa: {}", dtmId, e.getMessage());
        }
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "<json-error:" + e.getMessage() + ">";
        }
    }
}