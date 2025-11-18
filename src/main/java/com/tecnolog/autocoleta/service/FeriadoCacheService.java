package com.tecnolog.autocoleta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Year;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FeriadoCacheService {

    private static final Logger log = LoggerFactory.getLogger(FeriadoCacheService.class);
    private final RestTemplate restTemplate;
    
    private final Set<LocalDate> feriadosCache = ConcurrentHashMap.newKeySet();

    public FeriadoCacheService() {
        this.restTemplate = new RestTemplate(); 
        
        atualizarCacheFeriados(); 
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void atualizarCacheFeriados() {
        log.info("Iniciando atualização do cache de feriados em memória...");
        try {
            int anoAtual = Year.now().getValue();
            Set<LocalDate> feriadosTemp = new HashSet<>();

            feriadosTemp.addAll(buscarFeriadosDoAno(anoAtual));
            feriadosTemp.addAll(buscarFeriadosDoAno(anoAtual + 1));

            feriadosCache.clear();
            feriadosCache.addAll(feriadosTemp);

            log.info("Cache de feriados em memória atualizado com {} datas.", feriadosCache.size());
        } catch (Exception e) {
            log.error("Falha ao atualizar cache de feriados em memória: {}", e.getMessage(), e);
        }
    }

    private Set<LocalDate> buscarFeriadosDoAno(int ano) {
        String url = "https://brasilapi.com.br/api/feriados/v1/" + ano;
        Set<LocalDate> feriadosDoAno = new HashSet<>();
        try {
            FeriadoBrasilAPI[] feriados = restTemplate.getForObject(url, FeriadoBrasilAPI[].class);
            if (feriados != null) {
                for (FeriadoBrasilAPI f : feriados) {
                    feriadosDoAno.add(LocalDate.parse(f.date));
                }
            }
            log.info("Buscados {} feriados da API para o ano {}.", feriadosDoAno.size(), ano);
        } catch (Exception e) {
            log.error("Falha ao buscar feriados da API para o ano {}: {}", ano, e.getMessage());
        }
        return feriadosDoAno;
    }

    public boolean isFimDeSemana(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    public boolean isFeriado(LocalDate data) {
        return feriadosCache.contains(data);
    }

    public boolean isDiaNaoUtil(LocalDate data) {
        return isFimDeSemana(data) || isFeriado(data);
    }

    public LocalDate getProximoDiaUtil(LocalDate dataBase) {
        LocalDate dataProcurada = dataBase;
        
        while (isDiaNaoUtil(dataProcurada)) {
            dataProcurada = dataProcurada.plusDays(1);
        }
        return dataProcurada;
    }

    private static class FeriadoBrasilAPI {
        public String date; 
        public String name;
        public String type;
    }
}