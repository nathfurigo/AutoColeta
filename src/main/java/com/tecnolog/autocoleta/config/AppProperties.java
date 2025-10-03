package com.tecnolog.autocoleta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.tecnolog.autocoleta.domain.Modal;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Dtm dtm = new Dtm();
    private SalvarColeta salvarColeta = new SalvarColeta();
    private Defaults defaults = new Defaults();

    public Dtm getDtm() { return dtm; }
    public void setDtm(Dtm dtm) { this.dtm = dtm; }

    public SalvarColeta getSalvarColeta() { return salvarColeta; }
    public void setSalvarColeta(SalvarColeta salvarColeta) { this.salvarColeta = salvarColeta; }

    public Defaults getDefaults() { return defaults; }
    public void setDefaults(Defaults defaults) { this.defaults = defaults; }

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

    /** Defaults para preenchimento do payload quando o DTM não trouxer valores */
    public static class Defaults {
        private Integer idRemetente;
        private Integer idDestinatario;
        private Integer idTomador;
        private Integer idLocalColeta;

        private Integer idTipoColetaDefault = 3; 
        private Modal modal = Modal.RODOVIARIO;
        public Modal getModal() { return modal; }
        public void setModal(Modal modal) { this.modal = modal; }        
        private Integer idFilialResposavel;     
        private Integer idEnderecoCidade;        

        private String hrInicio = "08:00";
        private String hrFim    = "17:00";

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

         public static enum ModalType { RODOVIARIO, AEREO }

        public Integer getIdFilialResposavel() { return idFilialResposavel; }
        public void setIdFilialResposavel(Integer idFilialResposavel) { this.idFilialResposavel = idFilialResposavel; }

        public Integer getIdEnderecoCidade() { return idEnderecoCidade; }
        public void setIdEnderecoCidade(Integer idEnderecoCidade) { this.idEnderecoCidade = idEnderecoCidade; }

        public String getHrInicio() { return hrInicio; }
        public void setHrInicio(String hrInicio) { this.hrInicio = hrInicio; }

        public String getHrFim() { return hrFim; }
        public void setHrFim(String hrFim) { this.hrFim = hrFim; }
    }
    private Pedidos pedidos = new Pedidos();
    public Pedidos getPedidos(){ return pedidos; }
    public void setPedidos(Pedidos p){ this.pedidos = p; }

    public static class Pedidos {
    private String baseUrl;
    private Integer connectTimeoutMs = 5000;
    private Integer readTimeoutMs = 30000;
    }

}