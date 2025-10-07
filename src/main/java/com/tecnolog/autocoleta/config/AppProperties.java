package com.tecnolog.autocoleta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Dtm dtm = new Dtm();
    private final SalvarColeta salvarColeta = new SalvarColeta();
    private final SalvarOcorrencia salvarOcorrencia = new SalvarOcorrencia();
    private final Defaults defaults = new Defaults();
    private final Scheduler scheduler = new Scheduler();

    public Dtm getDtm() { return dtm; }
    public SalvarColeta getSalvarColeta() { return salvarColeta; }
    public SalvarOcorrencia getSalvarOcorrencia() { return salvarOcorrencia; }
    public Defaults getDefaults() { return defaults; }
    public Scheduler getScheduler() { return scheduler; }

    public static class Scheduler {
        private int batchSize;
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }

    public static class Dtm {
        private String view;
        private String lockTable;
        public String getView() { return view; }
        public void setView(String view) { this.view = view; }
        public String getLockTable() { return lockTable; }
        public void setLockTable(String lockTable) { this.lockTable = lockTable; }
    }

    public static class SalvarColeta {
        private String baseUrl;
        private String tokenHash;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getTokenHash() { return tokenHash; }
        public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    }

    public static class SalvarOcorrencia {
        private String baseUrl;
        private String systemToken;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getSystemToken() { return systemToken; }
        public void setSystemToken(String systemToken) { this.systemToken = systemToken; }
    }

    public static class Defaults {
        private Integer idRemetente;
        private Integer idDestinatario;
        private Integer idTomador;
        private Integer idLocalColeta;
        private Integer idTipoColetaDefault;
        private Modal modal;
        private Integer idFilialResposavel;
        private Integer idEnderecoCidade;
        private Integer idAgente;
        private String hrInicio;
        private String hrFim;
        
        public enum Modal { RODOVIARIO, AEREO }

        public Integer getIdRemetente() { return idRemetente; }
        public void setIdRemetente(Integer idRemetente) { this.idRemetente = idRemetente; }
        public Integer getIdDestinatario() { return idDestinatario; }
        public void setIdDestinatario(Integer idDestinatario) { this.idDestinatario = idDestinatario; }
        public Integer getIdTomador() { return idTomador; }
        public void setIdTomador(Integer idTomador) { this.idTomador = idTomador; }
        public Integer getIdLocalColeta() { return idLocalColeta; }
        public void setIdLocalColeta(Integer idLocalColeta) { this.idLocalColeta = idLocalColeta; }
        public Integer getIdTipoColetaDefault() { return idTipoColetaDefault; }
        public void setIdTipoColetaDefault(Integer idTipoColetaDefault) { this.idTipoColetaDefault = idTipoColetaDefault; }
        public Modal getModal() { return modal; }
        public void setModal(Modal modal) { this.modal = modal; }
        public Integer getIdFilialResposavel() { return idFilialResposavel; }
        public void setIdFilialResposavel(Integer idFilialResposavel) { this.idFilialResposavel = idFilialResposavel; }
        public Integer getIdEnderecoCidade() { return idEnderecoCidade; }
        public void setIdEnderecoCidade(Integer idEnderecoCidade) { this.idEnderecoCidade = idEnderecoCidade; }
        public Integer getIdAgente() { return idAgente; }
        public void setIdAgente(Integer idAgente) { this.idAgente = idAgente; }
        public String getHrInicio() { return hrInicio; }
        public void setHrInicio(String hrInicio) { this.hrInicio = hrInicio; }
        public String getHrFim() { return hrFim; }
        public void setHrFim(String hrFim) { this.hrFim = hrFim; }
    }
}