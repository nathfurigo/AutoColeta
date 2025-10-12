package com.tecnolog.autocoleta.dtm;

import com.tecnolog.autocoleta.config.AppProperties;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

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
        // Etapa 1: Usa a busca por palavras-chave para encontrar os IDs de Pessoas (Remetente, etc).
        model.setIdRemetente(findPessoaIdByKeywords(model.getDsProcurarPor()));
        model.setIdTomador(model.getIdRemetente());
        model.setIdLocalColeta(model.getIdRemetente());

        // Busca o ID do Agente usando o novo método que aceita nome OU email.
        model.setIdAgente(findAgenteIdByNomeOuEmail(model.getDsAgenteNome(), model.getDsAgenteEmail()));

        // Demais buscas continuam como antes
        model.setIdTipoColeta(findTipoColetaIdByName(model.getDsTipoColeta()));
        model.setIdEmbalagem(findEmbalagemIdComDePara(model.getDsEmbalagem()));
        model.setIdNaturezaCarga(findNaturezaIdComDePara(model.getDsNaturezaCarga()));

        // Etapa 2: Busca de padrões do remetente (fallback)
        if (model.getIdRemetente() != null && (model.getIdEmbalagem() == null || model.getIdNaturezaCarga() == null)) {
            try {
                String sql = "SELECT id_Embalagem, id_NaturezaMercadoria FROM tbdRemetente WHERE id_Remetente = ?";
                jdbc.queryForObject(sql, (rs, rowNum) -> {
                    if (model.getIdEmbalagem() == null) model.setIdEmbalagem(rs.getInt("id_Embalagem"));
                    if (model.getIdNaturezaCarga() == null) model.setIdNaturezaCarga(rs.getInt("id_NaturezaMercadoria"));
                    return model;
                }, model.getIdRemetente());
                log.info("DTM {}: Usados padrões de Embalagem/Natureza para o remetente ID {}.", model.getIdDtm(), model.getIdRemetente());
            } catch (EmptyResultDataAccessException e) {
                log.warn("DTM {}: Nenhuma configuração padrão de Embalagem/Natureza encontrada para o remetente ID {}.", model.getIdDtm(), model.getIdRemetente());
            }
        }

        // Etapa 3: Fallback final com os padrões da aplicação
        fillDefaultsIfNull(model);
    }

    private Integer findAgenteIdByNomeOuEmail(String nome, String email) {
        if ((nome == null || nome.isBlank()) && (email == null || email.isBlank())) {
            return null;
        }

        String sqlBase = "SELECT TOP 1 id_Pessoa FROM tbdPessoa WHERE ";
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (nome != null && !nome.isBlank()) {
            whereClause.append("ds_Pessoa = ?");
            params.add(nome);
        }

        if (email != null && !email.isBlank()) {
            if (!whereClause.isEmpty()) {
                whereClause.append(" OR ");
            }
            whereClause.append("cd_Email = ?");
            params.add(email);
        }

        if (params.isEmpty()) {
            return null;
        }

        String finalSql = sqlBase + whereClause.toString();

        try {
            Integer id = jdbc.queryForObject(finalSql, Integer.class, params.toArray());
            log.info("ID do Agente encontrado: {} (busca por nome='{}' ou email='{}')", id, nome, email);
            return id;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID de Agente para nome='{}' ou email='{}'", nome, email);
            return null;
        }
    }

    private Integer findPessoaIdByKeywords(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            return null;
        }
        String[] keywords = nomeCompleto.trim().split("\\s+");
        if (keywords.length == 0) {
            return null;
        }
        StringBuilder whereClause = new StringBuilder();
        List<String> params = new ArrayList<>();
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            if (keyword.length() <= 2 || keyword.equalsIgnoreCase("de") || keyword.equalsIgnoreCase("do")) {
                continue;
            }
            if (!params.isEmpty()) {
                whereClause.append(" AND ");
            }
            whereClause.append("(ds_RazaoSocial LIKE ? OR ds_Pessoa LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (params.isEmpty()){
            log.warn("Nenhuma palavra-chave válida encontrada em '{}' para a busca.", nomeCompleto);
            return null;
        }
        String sql = "SELECT TOP 1 id_Pessoa FROM tbdPessoa WHERE " + whereClause.toString();
        try {
            Integer foundId = jdbc.queryForObject(sql, Integer.class, params.toArray());
            log.info("Busca por palavras-chave para '{}' encontrou o ID: {}", nomeCompleto, foundId);
            return foundId;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID de pessoa usando as palavras-chave de '{}'.", nomeCompleto);
            return null;
        }
    }

    private Integer findEmbalagemIdComDePara(String nomeOrigem) {
        if (nomeOrigem == null || nomeOrigem.isBlank()) return null;
        String nomeDestino = nomeOrigem;
        try {
            String deParaSql = "SELECT ds_nome_destino_sqlserver FROM tbd_de_para_embalagem WHERE ds_nome_origem_postgres = ?";
            nomeDestino = jdbc.queryForObject(deParaSql, String.class, nomeOrigem);
            log.info("Mapeamento De-Para encontrado para embalagem '{}' -> '{}'", nomeOrigem, nomeDestino);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi encontrado mapeamento 'De-Para' para a embalagem de origem '{}'.", nomeOrigem);
        }
        try {
            String sql = "SELECT id_Embalagem FROM tbdEmbalagem WHERE ds_Embalagem = ?";
            return jdbc.queryForObject(sql, Integer.class, nomeDestino);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a Embalagem '{}'.", nomeDestino);
            return null;
        }
    }

    /**
     * --- MÉTODO AJUSTADO ---
     * Busca o ID de uma natureza de carga. Primeiro tenta um mapeamento 'De-Para'
     * e depois busca na tabela de natureza usando LIKE para uma busca flexível.
     * @param nomeOrigem O nome da natureza vindo do sistema de origem.
     * @return O ID encontrado ou null.
     */
    private Integer findNaturezaIdComDePara(String nomeOrigem) {
        if (nomeOrigem == null || nomeOrigem.isBlank()) return null;

        String nomeDestino = nomeOrigem;
        try {
            // Etapa 1: Tenta encontrar um mapeamento "De-Para"
            String deParaSql = "SELECT ds_nome_destino_sqlserver FROM tbd_de_para_natureza WHERE ds_nome_origem_postgres = ?";
            nomeDestino = jdbc.queryForObject(deParaSql, String.class, nomeOrigem);
            log.info("Mapeamento De-Para encontrado para natureza '{}' -> '{}'", nomeOrigem, nomeDestino);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi encontrado mapeamento 'De-Para' para a natureza '{}'. Usando nome original.", nomeOrigem);
        }

        try {
            // Etapa 2: Busca o ID na tabela final usando LIKE para flexibilidade
            // ALTERAÇÃO: Trocado '=' por 'LIKE' e adicionado 'TOP 1' para segurança.
            String sql = "SELECT TOP 1 id_Natureza FROM tbdNatureza WHERE ds_Natureza LIKE ?";
            
            // ALTERAÇÃO: Adicionados wildcards '%' para a busca com LIKE.
            return jdbc.queryForObject(sql, Integer.class, "%" + nomeDestino + "%");
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a Natureza da Carga contendo '{}'.", nomeDestino);
            return null;
        }
    }

    private Integer findTipoColetaIdByName(String nome) {
        if (nome == null || nome.isBlank()) return null;
        try {
            String sql = "SELECT TOP 1 id_TipoPedidoColeta FROM tbdTipoPedidoColeta WHERE ds_TipoPedidoColeta LIKE ?";
            return jdbc.queryForObject(sql, Integer.class, "%" + nome + "%");
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para o Tipo de Coleta contendo '{}'.", nome);
            return null;
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

        if (model.getIdNaturezaCarga() == null) log.error("DTM {}: ID da Natureza da Carga é obrigatório e não foi encontrado.", model.getIdDtm());
        if (model.getIdEmbalagem() == null) log.error("DTM {}: ID da Embalagem é obrigatório e não foi encontrado.", model.getIdDtm());

        if (model.getHrColetaFim() == null || model.getHrColetaFim().isBlank()) {
            model.setHrColetaFim(appProperties.getDefaults().getHrFim());
        }
        if (model.getTpModal() == null) {
            model.setTpModal(appProperties.getDefaults().getModal() == AppProperties.Defaults.Modal.AEREO ? 2 : 1);
        }
    }
}