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

    public void preencherDadosFaltantes(SalvaColetaModel model) {
        
        // 1. Busca os IDs de Remetente, Destinatário e Tomador na tbdPessoa
        // A view do postgres já fornece os nomes, que usamos aqui para buscar os IDs.
        model.setIdRemetente(findPessoaIdByName(model.getDsProcurarPor(), appProperties.getDefaults().getIdRemetente()));
        // Supondo que o nome do destinatário venha de outro campo (não populado pela view). Se não, a lógica precisa de ajuste.
        // model.setIdDestinatario(findPessoaIdByName(model.getDsNomeDestinatario(), appProperties.getDefaults().getIdDestinatario()));
        model.setIdTomador(findPessoaIdByName(model.getDsProcurarPor(), appProperties.getDefaults().getIdTomador()));
        model.setIdLocalColeta(model.getIdRemetente()); // Frequentemente o local de coleta é o próprio remetente.

        // 2. Com o ID do Remetente, busca suas configurações padrão na tbdRemetente
        if (model.getIdRemetente() != null) {
            try {
                String sql = "SELECT id_Embalagem, id_Natureza FROM tbdRemetente WHERE id_Remetente = ?";
                jdbc.queryForObject(sql, (rs, rowNum) -> {
                    model.setIdEmbalagem(rs.getInt("id_Embalagem"));
                    model.setIdNaturezaCarga(rs.getInt("id_Natureza"));
                    return model;
                }, model.getIdRemetente());
                log.info("DTM {}: Encontrados id_Embalagem e id_Natureza padrão para o remetente {}.", model.getIdDtm(), model.getIdRemetente());
            } catch (EmptyResultDataAccessException e) {
                log.warn("DTM {}: Nenhuma configuração padrão encontrada para o remetente {}.", model.getIdDtm(), model.getIdRemetente());
            }
        }

        // 3. Preenche outros campos com valores padrão se ainda estiverem nulos
        fillDefaultsIfNull(model);
    }

    /**
     * Busca o ID de uma pessoa (cliente/remetente/etc.) pelo nome.
     * Se não encontrar, retorna o ID padrão.
     */
    private Integer findPessoaIdByName(String nome, Integer defaultId) {
        if (nome == null || nome.isBlank()) {
            return defaultId;
        }
        try {
            // Usa ds_RazaoSocial ou ds_Pessoa dependendo de qual campo contém o nome na sua base
            String sql = "SELECT id_Pessoa FROM tbdPessoa WHERE ds_RazaoSocial = ? OR ds_Pessoa = ?";
            return jdbc.queryForObject(sql, Integer.class, nome, nome);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a pessoa '{}'. Usando o padrão ({}).", nome, defaultId);
            return defaultId;
        }
    }

    /**
     * Aplica os valores do arquivo de configuração para qualquer campo que ainda seja nulo.
     */
    private void fillDefaultsIfNull(SalvaColetaModel model) {
        if (model.getIdRemetente() == null) model.setIdRemetente(appProperties.getDefaults().getIdRemetente());
        if (model.getIdDestinatario() == null) model.setIdDestinatario(appProperties.getDefaults().getIdDestinatario());
        if (model.getIdTomador() == null) model.setIdTomador(appProperties.getDefaults().getIdTomador());
        if (model.getIdFilialResposavel() == null) model.setIdFilialResposavel(appProperties.getDefaults().getIdFilialResposavel());
        if (model.getIdLocalColeta() == null) model.setIdLocalColeta(appProperties.getDefaults().getIdLocalColeta());
        if (model.getIdEnderecoCidade() == null) model.setIdEnderecoCidade(appProperties.getDefaults().getIdEnderecoCidade());
        if (model.getIdTipoColeta() == null) model.setIdTipoColeta(appProperties.getDefaults().getIdTipoColetaDefault());
        if (model.getIdAgente() == null) model.setIdAgente(appProperties.getDefaults().getIdAgente());
        // ATENÇÃO: Adicionar padrões para Natureza e Embalagem em AppProperties se forem necessários como fallback.
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