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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Repository
public class SqlServerRepository {

    private static final Logger log = LoggerFactory.getLogger(SqlServerRepository.class);
    private final JdbcTemplate jdbc;
    private final AppProperties appProperties;
    private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("[^\\d]");

    private static final Map<String, String> NATUREZA_KEYWORD_MAP;
    static {
        NATUREZA_KEYWORD_MAP = new HashMap<>();
        NATUREZA_KEYWORD_MAP.put("PARAF", "PEÇAS"); // Parafuso, PARAF.
        NATUREZA_KEYWORD_MAP.put("TUBO", "PEÇAS");
        NATUREZA_KEYWORD_MAP.put("FLANGE", "PEÇAS");
        NATUREZA_KEYWORD_MAP.put("JUNTA", "PEÇAS");
        NATUREZA_KEYWORD_MAP.put("CONECTOR", "PEÇAS");
        NATUREZA_KEYWORD_MAP.put("VÁLVULA", "PEÇAS");
        NATUREZA_KEYWORD_MAP.put("LUVA", "PEÇAS");
        NATUREZA_KEYWORD_MAP.put("CALÇA", "VESTUARIO");
        NATUREZA_KEYWORD_MAP.put("MACACÃO", "VESTUARIO");
        NATUREZA_KEYWORD_MAP.put("CAMISA", "VESTUARIO");
        NATUREZA_KEYWORD_MAP.put("TERMINAL", "MATERIAL ELETRICO");
        NATUREZA_KEYWORD_MAP.put("CABO", "MATERIAL ELETRICO");
    }

    private static final Map<String, String> PESSOA_ALIAS_MAP;
        static {
            PESSOA_ALIAS_MAP = new HashMap<>();
            // Mapeie "apelidos" que chegam na DTM para o nome exato como está no banco de destino

            // --- MAPEAMENTOS CORRIGIDOS COM BASE NA SUA PESQUISA ---
            PESSOA_ALIAS_MAP.put("REGAP", "PETROLEO BRASILEIRO - REGAP - BETIM"); // ID 1052
            PESSOA_ALIAS_MAP.put("REPLAN", "PETROLEO BRASILEIRO SA - PAULINIA"); // ID 1054
            PESSOA_ALIAS_MAP.put("REDUC", "PETROLEO BRASILEIRO S.A - REDUC"); // ID 1066
            PESSOA_ALIAS_MAP.put("REVAP", "PETRO (REVAP)"); // Nome a ser confirmado
            PESSOA_ALIAS_MAP.put("AEROPORTO GALEAO", "LIDER SIGNATURE S/A - GALEAO"); // ID 40900
            PESSOA_ALIAS_MAP.put("PETROBRAS CENPES - CENTRO 0054", "PETROLEO BRASILEIRO S/A - CENPES");
        }
    public SqlServerRepository(@Qualifier("sqlServerJdbcTemplate") JdbcTemplate jdbc, AppProperties appProperties) {
        this.jdbc = jdbc;
        this.appProperties = appProperties;
    }

    public void preencherDadosFaltantes(SalvaColetaModel model) {
        model.setIdRemetente(findPessoaId(model.getDsRemetente(), model.getCdRemetenteCnpj()));
        model.setIdDestinatario(findPessoaId(model.getDsDestinatario(), model.getCdDestinatarioCnpj()));
        model.setIdTomador(findPessoaId(model.getDsTomador(), model.getCdTomadorCnpj()));

        if (model.getIdLocalColeta() == null) {
            model.setIdLocalColeta(model.getIdRemetente());
        }

        model.setIdAgente(findAgenteIdByNomeOuEmail(model.getDsAgenteNome(), model.getDsAgenteEmail()));
        model.setIdTipoColeta(findTipoColetaIdByName(model.getDsTipoColeta()));
        model.setIdEmbalagem(findEmbalagemIdComDePara(model.getDsEmbalagem()));
        model.setIdNaturezaCarga(findNaturezaIdComDePara(model.getDsNaturezaCarga()));

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

        fillDefaultsIfNull(model);
    }

    private Integer findAgenteIdByNomeOuEmail(String nome, String email) {
        if ((nome == null || nome.isBlank()) && (email == null || email.isBlank())) {
            return null;
        }

        String sql = "SELECT TOP 1 p.id_Pessoa FROM tbdPessoa p INNER JOIN tbdAgente a ON p.id_Pessoa = a.id_Agente WHERE ISNULL(a.tp_InativoErrado, 'N') <> 'S' AND (";
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (nome != null && !nome.isBlank()) {
            whereClause.append("LOWER(p.ds_Pessoa) COLLATE Latin1_General_CI_AI LIKE ?");
            params.add("%" + nome.toLowerCase() + "%");
        }

        if (email != null && !email.isBlank()) {
            if (!whereClause.isEmpty()) {
                whereClause.append(" OR ");
            }
            whereClause.append("LOWER(p.cd_Email) = ?");
            params.add(email.toLowerCase());
        }

        if (params.isEmpty()) {
            return null;
        }

        String finalSql = sql + whereClause.toString() + ")";

        try {
            Integer id = jdbc.queryForObject(finalSql, Integer.class, params.toArray());
            log.info("ID do Agente encontrado: {} (busca por nome='{}' ou email='{}')", id, nome, email);
            return id;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID de Agente para nome='{}' ou email='{}'", nome, email);
            return null;
        }
    }

    private Integer findPessoaId(String nome, String cnpj) {
        if ((nome == null || nome.isBlank()) && (cnpj == null || cnpj.isBlank())) {
            return null;
        }

        String nomeBusca = nome;
        if (nomeBusca != null && PESSOA_ALIAS_MAP.containsKey(nomeBusca.toUpperCase())) {
            String nomeTraduzido = PESSOA_ALIAS_MAP.get(nomeBusca.toUpperCase());
            log.info("Apelido de Pessoa '{}' traduzido para busca como '{}'", nome, nomeTraduzido);
            nomeBusca = nomeTraduzido;
        }

        String sqlBase = "SELECT TOP 1 id_Pessoa FROM tbdPessoa WHERE ";
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (nomeBusca != null && !nomeBusca.isBlank()) {
            whereClause.append("(LOWER(ds_Pessoa) COLLATE Latin1_General_CI_AI LIKE ? OR LOWER(ds_RazaoSocial) COLLATE Latin1_General_CI_AI LIKE ?)");
            String termoBusca = "%" + nomeBusca.toLowerCase() + "%";
            params.add(termoBusca);
            params.add(termoBusca);
        }

        if (cnpj != null && !cnpj.isBlank()) {
            if (!whereClause.isEmpty()) {
                whereClause.append(" OR ");
            }
            String cleanCnpj = NON_DIGIT_PATTERN.matcher(cnpj).replaceAll("");
            whereClause.append("cd_CGCCPF = ?");
            params.add(cleanCnpj);
        }

        if (params.isEmpty()) {
            return null;
        }

        String finalSql = sqlBase + whereClause.toString();

        try {
            Integer id = jdbc.queryForObject(finalSql, Integer.class, params.toArray());
            log.info("ID de Pessoa encontrado: {} (busca por nome='{}' ou cnpj='{}')", id, nome, cnpj);
            return id;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID de Pessoa para nome='{}' ou cnpj='{}'", nome, cnpj);
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
            String sql = "SELECT TOP 1 id_Embalagem FROM tbdEmbalagem WHERE LOWER(ds_Embalagem) COLLATE Latin1_General_CI_AI LIKE ?";
            return jdbc.queryForObject(sql, Integer.class, "%" + nomeDestino.toLowerCase() + "%");
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a Embalagem contendo '{}'.", nomeDestino);
            return null;
        }
    }

    private Integer findNaturezaIdComDePara(String nomeOrigem) {
        if (nomeOrigem == null || nomeOrigem.isBlank()) return null;

        String nomeDestino = nomeOrigem;
        try {
            String deParaSql = "SELECT ds_nome_destino_sqlserver FROM tbd_de_para_natureza WHERE ds_nome_origem_postgres = ?";
            nomeDestino = jdbc.queryForObject(deParaSql, String.class, nomeOrigem);
            log.info("Mapeamento De-Para encontrado para natureza '{}' -> '{}'", nomeOrigem, nomeDestino);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi encontrado mapeamento 'De-Para' para a natureza '{}'. Tentando mapeamento por palavra-chave.", nomeOrigem);

            String nomeOrigemUpper = nomeOrigem.toUpperCase();
            for (Map.Entry<String, String> entry : NATUREZA_KEYWORD_MAP.entrySet()) {
                if (nomeOrigemUpper.contains(entry.getKey())) {
                    nomeDestino = entry.getValue();
                    log.info("Mapeamento por palavra-chave encontrado para natureza '{}' -> '{}'", nomeOrigem, nomeDestino);
                    break;
                }
            }
        }

        try {
            String sql = "SELECT TOP 1 id_NaturezaMercadoria FROM tbdNaturezaMercadoria WHERE LOWER(ds_NaturezaMercadoria) COLLATE Latin1_General_CI_AI LIKE ?";
            return jdbc.queryForObject(sql, Integer.class, "%" + nomeDestino.toLowerCase() + "%");
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a Natureza da Carga contendo '{}'.", nomeDestino);
            return null;
        }
    }

    private Integer findTipoColetaIdByName(String nome) {
        if (nome == null || nome.isBlank()) return null;

        String termoBusca = nome.toUpperCase();
        if (termoBusca.endsWith("O") || termoBusca.endsWith("A")) {
            termoBusca = termoBusca.substring(0, termoBusca.length() - 1);
        }

        try {
            String sql = "SELECT TOP 1 id_TipoPedidoColeta FROM tbdTipoPedidoColeta WHERE LOWER(ds_TipoPedidoColeta) COLLATE Latin1_General_CI_AI LIKE ?";
            return jdbc.queryForObject(sql, Integer.class, termoBusca.toLowerCase() + "%");
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