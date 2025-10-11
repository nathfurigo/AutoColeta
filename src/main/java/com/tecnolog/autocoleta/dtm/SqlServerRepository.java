package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqlServerRepository {

    private static final Logger log = LoggerFactory.getLogger(SqlServerRepository.class);
    private final JdbcTemplate jdbc;
    private final AppProperties appProperties;

    public SqlServerRepository(@Qualifier("sqlServerJdbcTemplate") JdbcTemplate jdbc, AppProperties appProperties) {
        this.jdbc = jdbc;
        this.appProperties = appProperties;
    }

    /**
     * Orquestra o preenchimento de IDs e dados faltantes no modelo da coleta,
     * imitando a lógica do front-end.
     */
    public void preencherDadosFaltantes(SalvaColetaModel model) {
        
        // --- LÓGICA AJUSTADA ---

        // Etapa 1: Busca o Tomador/Pagador pelo CNPJ fixo, assim como a tela.
        model.setIdTomador(findPessoaIdByCnpj("33000167000101", appProperties.getDefaults().getIdTomador()));

        // Etapa 2: Para Remetente e Destinatário, como a tela usa busca manual, 
        // a automação usará os IDs padrão para garantir consistência.
        model.setIdRemetente(findPessoaIdByName(model.getDsProcurarPor(), appProperties.getDefaults().getIdRemetente()));
        model.setIdDestinatario(appProperties.getDefaults().getIdDestinatario()); // Usando o padrão
        model.setIdLocalColeta(model.getIdRemetente()); // O local de coleta é o próprio remetente.

        // Etapa 3: Busca outros IDs com mais flexibilidade
        model.setIdAgente(findAgenteIdByName(model.getDsAgente(), null));
        model.setIdTipoColeta(findTipoColetaIdByName(model.getDsTipoColeta(), null)); // Agora com busca flexível
        model.setIdEmbalagem(findEmbalagemIdByName(model.getDsEmbalagem(), null));
        model.setIdNaturezaCarga(findNaturezaIdByName(model.getDsNaturezaCarga(), null));

        // Etapa 4: Busca padrões do Remetente se necessário (lógica mantida)
        if (model.getIdRemetente() != null && (model.getIdEmbalagem() == null || model.getIdNaturezaCarga() == null)) {
            try {
                String sql = "SELECT id_Embalagem, id_Natureza FROM tbdRemetente WHERE id_Remetente = ?";
                jdbc.queryForObject(sql, (rs, rowNum) -> {
                    if (model.getIdEmbalagem() == null) model.setIdEmbalagem(rs.getInt("id_Embalagem"));
                    if (model.getIdNaturezaCarga() == null) model.setIdNaturezaCarga(rs.getInt("id_Natureza"));
                    return model;
                }, model.getIdRemetente());
                log.info("DTM {}: Usados padrões de Embalagem/Natureza para o remetente {}.", model.getIdDtm(), model.getIdRemetente());
            } catch (EmptyResultDataAccessException e) {
                log.warn("DTM {}: Nenhuma configuração padrão de Embalagem/Natureza encontrada para o remetente {}.", model.getIdDtm(), model.getIdRemetente());
            }
        }

        // Etapa 5: Preenche com padrões globais se ainda estiver nulo
        fillDefaultsIfNull(model);
    }

    private Integer findPessoaIdByName(String nome, Integer defaultId) {
        if (nome == null || nome.isBlank()) return defaultId;
        try {
            String sql = "SELECT id_Pessoa FROM tbdPessoa WHERE ds_RazaoSocial = ? OR ds_Pessoa = ?";
            return jdbc.queryForObject(sql, Integer.class, nome, nome);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a pessoa '{}'. Usando o padrão ({}).", nome, defaultId);
            return defaultId;
        }
    }
    
    // NOVO MÉTODO: Busca pessoa pelo CNPJ
    private Integer findPessoaIdByCnpj(String cnpj, Integer defaultId) {
        if (cnpj == null || cnpj.isBlank()) return defaultId;
        try {
            String sql = "SELECT id_Pessoa FROM tbdPessoa WHERE cd_CGCCPF = ?";
            return jdbc.queryForObject(sql, Integer.class, cnpj);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a pessoa com CNPJ '{}'. Usando o padrão ({}).", cnpj, defaultId);
            return defaultId;
        }
    }

    private Integer findAgenteIdByName(String nome, Integer defaultId) {
        if (nome == null || nome.isBlank()) return defaultId;
        log.warn("Lógica de busca para Agente '{}' desativada. Usando o padrão ({}).", nome, defaultId);
        return defaultId;
    }
    
    private Integer findTipoColetaIdByName(String nome, Integer defaultId) {
        if (nome == null || nome.isBlank()) return defaultId;
        try {
            // AJUSTE FINAL: Usando LIKE para busca flexível
            String sql = "SELECT id_TipoPedidoColeta FROM tbdTipoPedidoColeta WHERE ds_TipoPedidoColeta LIKE ?";
            return jdbc.queryForObject(sql, (rs, rowNum) -> rs.getInt("id_TipoPedidoColeta"), "%" + nome + "%");
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para o Tipo de Coleta '{}'. Usando o padrão ({}).", nome, defaultId);
            return defaultId;
        }
    }

    private Integer findEmbalagemIdByName(String nome, Integer defaultId) {
        if (nome == null || nome.isBlank()) return defaultId;
        try {
            String sql = "SELECT id_Embalagem FROM tbdEmbalagem WHERE ds_Embalagem = ?";
            return jdbc.queryForObject(sql, Integer.class, nome);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a Embalagem '{}'. Usando o padrão ({}).", nome, defaultId);
            return defaultId;
        }
    }

    private Integer findNaturezaIdByName(String nome, Integer defaultId) {
        if (nome == null || nome.isBlank()) return defaultId;
        try {
            String sql = "SELECT id_NaturezaMercadoria FROM tbdNaturezaMercadoria WHERE ds_NaturezaMercadoria = ?";
            return jdbc.queryForObject(sql, (rs, rowNum) -> rs.getInt("id_NaturezaMercadoria"), nome);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a Natureza da Carga '{}'. Usando o padrão ({}).", nome, defaultId);
            return defaultId;
        }
    }
    
    private void fillDefaultsIfNull(SalvaColetaModel model) {
        if (model.getIdRemetente() == null) model.setIdRemetente(appProperties.getDefaults().getIdRemetente());
        if (model.getIdDestinatario() == null) model.setIdDestinatario(appProperties.getDefaults().getIdDestinatario());
        if (model.getIdTomador() == null) model.setIdTomador(appProperties.getDefaults().getIdTomador());
        if (model.getIdFilialResposavel() == null) model.setIdFilialResposavel(appProperties.getDefaults().getIdFilialResposavel());
        if (model.getIdLocalColeta() == null) model.setIdLocalColeta(appProperties.getDefaults().getIdLocalColeta());
        if (model.getIdEnderecoCidade() == null) model.setIdEnderecoCidade(appProperties.getDefaults().getIdEnderecoCidade());
        if (model.getIdTipoColeta() == null) model.setIdTipoColeta(appProperties.getDefaults().getIdTipoColetaDefault());
        if (model.getIdAgente() == null) model.setIdAgente(appProperties.getDefaults().getIdAgente());
        
        if (model.getIdNaturezaCarga() == null) model.setIdNaturezaCarga(null);
        if (model.getIdEmbalagem() == null) model.setIdEmbalagem(null);
        
        if (model.getHrColetaFim() == null || model.getHrColetaFim().isBlank()) {
            model.setHrColetaFim(appProperties.getDefaults().getHrFim());
        }
        if (model.getTpModal() == null) {
            model.setTpModal(appProperties.getDefaults().getModal() == AppProperties.Defaults.Modal.AEREO ? 2 : 1);
        }
    }
}