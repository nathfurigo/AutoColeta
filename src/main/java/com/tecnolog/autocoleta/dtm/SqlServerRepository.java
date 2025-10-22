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
        NATUREZA_KEYWORD_MAP.put("BUCHA", "BUCHA");
        NATUREZA_KEYWORD_MAP.put("CAIXA PASSAG", "CAIXA DE PASSAGEM");
        NATUREZA_KEYWORD_MAP.put("VÁLV.ESF", "VALVULAS");
        NATUREZA_KEYWORD_MAP.put("VÁLVULA", "VALVULAS");
        NATUREZA_KEYWORD_MAP.put("VALVULA", "VALVULAS");
        NATUREZA_KEYWORD_MAP.put("ANEL O", "ANEL");
        NATUREZA_KEYWORD_MAP.put("DETECTOR DE GÁS", "DETECTOR DE GAS EM GERAL");
        NATUREZA_KEYWORD_MAP.put("DETECTOR", "DETECTORES");
        NATUREZA_KEYWORD_MAP.put("TARUGO", "TARUGO MACIÇO");
        NATUREZA_KEYWORD_MAP.put("UNIAO", "UNIAO DE ACO");
        NATUREZA_KEYWORD_MAP.put("NIPLE", "NIPLE");
        NATUREZA_KEYWORD_MAP.put("MACACAO", "MACACAO RF");
        NATUREZA_KEYWORD_MAP.put("ARRUELA", "ARRUELAS");
        NATUREZA_KEYWORD_MAP.put("CURVA TUBO", "PECAS P/ MAQUINAS INDUSTRIAIS");
        NATUREZA_KEYWORD_MAP.put("TUBO", "TUBOS");
        NATUREZA_KEYWORD_MAP.put("FLANGE", "FLANGE");
        NATUREZA_KEYWORD_MAP.put("JUNTA", "JUNTAS");
        NATUREZA_KEYWORD_MAP.put("CONECTOR", "CONECTOR");
        NATUREZA_KEYWORD_MAP.put("LUVA", "LUVAS");
        NATUREZA_KEYWORD_MAP.put("PLUGUE", "PLUG");
        NATUREZA_KEYWORD_MAP.put("ELEMENTO", "ELEMENTO FILTRANTE");
        NATUREZA_KEYWORD_MAP.put("PROTETOR", "EQUIPAMENTO DE SEGURANCA");
        NATUREZA_KEYWORD_MAP.put("CABO", "CABOS EM GERAL");
        NATUREZA_KEYWORD_MAP.put("RELÉ", "RELE");
        NATUREZA_KEYWORD_MAP.put("RELE", "RELE");
        NATUREZA_KEYWORD_MAP.put("CALÇA", "CONFECCOES");
        NATUREZA_KEYWORD_MAP.put("CAMISA", "CONFECCOES");
        NATUREZA_KEYWORD_MAP.put("JOGO", "PECAS P/ MAQUINAS INDUSTRIAIS");
        NATUREZA_KEYWORD_MAP.put("RAQUETE", "PECAS P/ MAQUINAS INDUSTRIAIS");
        NATUREZA_KEYWORD_MAP.put("LANTERNA", "LANTERNA");
        NATUREZA_KEYWORD_MAP.put("ABRAÇADEIRA", "ABRACADEIRA");
        NATUREZA_KEYWORD_MAP.put("BUJÃO", "BUJAO");
        NATUREZA_KEYWORD_MAP.put("CARTUCHO", "CARTUCHO");
        NATUREZA_KEYWORD_MAP.put("JAQUETA", "CONFECCOES");
        NATUREZA_KEYWORD_MAP.put("JOELHO", "JOELHO");
        NATUREZA_KEYWORD_MAP.put("KIT", "KITS");
        NATUREZA_KEYWORD_MAP.put("ROLAMENTO", "ROLAMENTOS");
        NATUREZA_KEYWORD_MAP.put("TAMPÃO", "TAMPAO");
        NATUREZA_KEYWORD_MAP.put("UNIÃO", "UNIAO DE ACO");
        NATUREZA_KEYWORD_MAP.put("MACACÃO", "MACACAO RF");
        NATUREZA_KEYWORD_MAP.put("PARAF. ESTOJO", "PARAFUSOS");
        NATUREZA_KEYWORD_MAP.put("PARAFUSO MÁQ", "PARAFUSOS");
        NATUREZA_KEYWORD_MAP.put("PARAF", "PARAFUSOS");
        NATUREZA_KEYWORD_MAP.put("TERMINAL CU", "MATERIAL ELETRICO");
        NATUREZA_KEYWORD_MAP.put("TERMINAL", "MATERIAL ELETRICO");
        NATUREZA_KEYWORD_MAP.put("PORCA P/TUB", "PORCA");
        NATUREZA_KEYWORD_MAP.put("PORCA", "PORCA");
        NATUREZA_KEYWORD_MAP.put("TUBO SIFÃO", "TUBOS");
        // NOVOS MAPEAMENTOS ADICIONADOS PARA REDUZIR FALHAS
        NATUREZA_KEYWORD_MAP.put("COLAR", "CONEXÕES");
        NATUREZA_KEYWORD_MAP.put("SOLENÓIDE", "VALVULAS");

    }
    private static final Map<String, String> PESSOA_ALIAS_MAP;
    static {
        PESSOA_ALIAS_MAP = new HashMap<>();
        PESSOA_ALIAS_MAP.put("REGAP", "PETROLEO BRASILEIRO - REGAP - BETIM");
        PESSOA_ALIAS_MAP.put("REPLAN", "PETROLEO BRASILEIRO SA - PAULINIA");
        PESSOA_ALIAS_MAP.put("REDUC", "PETROLEO BRASILEIRO S.A - REDUC");
        PESSOA_ALIAS_MAP.put("REVAP", "PETROLEO BRASILEIRO S/A S.J. DOS CAMPOS");
        PESSOA_ALIAS_MAP.put("RPBC", "PETROLEO BRASILEIRO S.A - CUBATAO");
        PESSOA_ALIAS_MAP.put("REMAN", "REFINARIA DA AMAZONIA - REAM");
        PESSOA_ALIAS_MAP.put("REPAR", "PETROLEO BRASILEIRO. S.A - REPAR");
        PESSOA_ALIAS_MAP.put("SIX", "PETROLEO BRASILEIRO - SIX - SÃO MATEUS DO SUL");
        PESSOA_ALIAS_MAP.put("LUBNOR", "PETROLEO BRASILEIRO S.A - LUBNOR");
        PESSOA_ALIAS_MAP.put("RLAM", "PETROLEO BRASILEIRO S.A - RLAM");
        PESSOA_ALIAS_MAP.put("RECAP", "PETROLEO BRASILEIRO S.A - MAUA");
        PESSOA_ALIAS_MAP.put("REFAP", "PETROLEO BRASILEIRO S/A RS");
        PESSOA_ALIAS_MAP.put("PETROBRAS CENPES - CENTRO 0054", "PETROLEO BRASILEIRO S/A - CENPES");
        PESSOA_ALIAS_MAP.put("CENPES - PORTARIA 4", "PETROLEO BRASILEIRO S/A - CENPES");
        PESSOA_ALIAS_MAP.put("ARM-MACAE", "PETROLEO BRASILEIRO S/A - MACAE AGENDAMENTO");
        PESSOA_ALIAS_MAP.put("UTE MARIO LAGO", "PETROLEO BRASILEIRO S.A - UTE LEONEL BRISOLA");
        PESSOA_ALIAS_MAP.put("UTGC LINHARES", "PETROLEO BRASILEIRO - LINHARES");
        PESSOA_ALIAS_MAP.put("UTE EUZÉBIO ROCHA", "PETROLEO BRASILEIRO S.A. - IPOJUCA");
        PESSOA_ALIAS_MAP.put("EDISEN", "PETROLEO BRASILEIRO");
        PESSOA_ALIAS_MAP.put("ARM-RIO", "PETROLEO BRASILEIRO S.A.(CORDOVIL) - (AGENDAMENTO");
        PESSOA_ALIAS_MAP.put("EDIVIT", "PETROLEO BRASILEIRO S/A");
        PESSOA_ALIAS_MAP.put("UTE TRES LAGOAS", "PETROLEO BRASILEIRO S.A");
        PESSOA_ALIAS_MAP.put("EDISER", "PETROLEO BRASILEIRO S/A - NATAL");
        PESSOA_ALIAS_MAP.put("BASE TAQUIPE", "PETROBRAS (TAQUIPE)");
        PESSOA_ALIAS_MAP.put("AEROPORTO GALEAO", "LIDER SIGNATURE S/A - GALEAO");
        PESSOA_ALIAS_MAP.put("WHITE MARTINS GASES INDUSTRIAIS LTDA - VINHEDO SP", "WHITE MARTINS");
    }
    public SqlServerRepository(@Qualifier("sqlServerJdbcTemplate") JdbcTemplate jdbc, AppProperties appProperties) {
        this.jdbc = jdbc;
        this.appProperties = appProperties;
    }

    public Integer findExistingColetaIdByDtm(String dtmId) {
        if (dtmId == null || dtmId.isBlank()) {
            return null;
        }
        String sql = """
            SELECT TOP 1 pc.id_PedidoColeta
            FROM tbdPedidoColeta pc
            LEFT JOIN tbdItemPedidoColeta ipc ON pc.id_PedidoColeta = ipc.id_PedidoColeta
            WHERE ipc.nr_Referencia = ? OR ipc.nr_PedidoCliente = ? OR pc.cm_PedidoColeta LIKE ?
            """;
        try {
            return jdbc.queryForObject(sql, Integer.class, dtmId, dtmId, "%" + dtmId + "%");
        } catch (EmptyResultDataAccessException e) {
            log.debug("Nenhuma coleta existente encontrada para a DTM {}", dtmId);
            return null;
        }
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
        if (email != null && !email.isBlank()) {
            whereClause.append("LOWER(p.cd_Email) = ?");
            params.add(email.toLowerCase());
        }
        if (nome != null && !nome.isBlank()) {
            if (!whereClause.isEmpty()) {
                whereClause.append(" OR ");
            }
            whereClause.append("LOWER(p.ds_Pessoa) COLLATE Latin1_General_CI_AI LIKE ?");
            params.add("%" + nome.toLowerCase() + "%");
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
            return null;
        }
    }

    private Integer findPessoaId(String nome, String cnpj) {
        if ((nome == null || nome.isBlank()) && (cnpj == null || cnpj.isBlank())) {
            return null;
        }

        if (cnpj != null && !cnpj.isBlank()) {
            try {
                String cleanCnpj = NON_DIGIT_PATTERN.matcher(cnpj).replaceAll("");
                String sqlCnpj = "SELECT TOP 1 id_Pessoa FROM tbdPessoa WHERE cd_CGCCPF = ?";
                Integer id = jdbc.queryForObject(sqlCnpj, Integer.class, cleanCnpj);
                log.info("ID de Pessoa encontrado via CNPJ: {} (para cnpj='{}')", id, cnpj);
                return id;
            } catch (EmptyResultDataAccessException e) {
                log.warn("Nenhuma Pessoa encontrada para o CNPJ '{}'. Prosseguindo para buscar pelo nome '{}'.", cnpj, nome);
            }
        }

        if (nome != null && !nome.isBlank()) {
            String nomeBusca = nome;
            if (PESSOA_ALIAS_MAP.containsKey(nomeBusca.toUpperCase())) {
                String nomeTraduzido = PESSOA_ALIAS_MAP.get(nomeBusca.toUpperCase());
                log.info("Apelido de Pessoa '{}' traduzido para busca como '{}'", nome, nomeTraduzido);
                nomeBusca = nomeTraduzido;
            }

            try {
                String sqlNome = "SELECT TOP 1 id_Pessoa FROM tbdPessoa WHERE (LOWER(ds_Pessoa) COLLATE Latin1_General_CI_AI LIKE ? OR LOWER(ds_RazaoSocial) COLLATE Latin1_General_CI_AI LIKE ?)";
                String termoBusca = "%" + nomeBusca.toLowerCase() + "%";
                Integer id = jdbc.queryForObject(sqlNome, Integer.class, termoBusca, termoBusca);
                log.info("ID de Pessoa encontrado via Nome: {} (para nome='{}')", id, nome);
                return id;
            } catch (EmptyResultDataAccessException e) {
                log.warn("Não foi possível encontrar um ID de Pessoa para nome='{}' (após falha na busca por CNPJ, se aplicável).", nome);
            }
        }

        return null;
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
        boolean deParaEncontrado = false;
        try {
            String deParaSql = "SELECT ds_nome_destino_sqlserver FROM tbd_de_para_natureza WHERE ds_nome_origem_postgres = ?";
            nomeDestino = jdbc.queryForObject(deParaSql, String.class, nomeOrigem);
            log.info("Mapeamento De-Para encontrado para natureza '{}' -> '{}'", nomeOrigem, nomeDestino);
            deParaEncontrado = true;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi encontrado mapeamento 'De-Para' para a natureza '{}'. Tentando mapeamento por palavra-chave.", nomeOrigem);
        }
        
        if (!deParaEncontrado) {
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
        if (model.getIdNaturezaCarga() == null) {
                log.warn("DTM {}: ID da Natureza da Carga não foi encontrado. Aplicando natureza genérica de fallback (ID: 1400).", model.getIdDtm());
                model.setIdNaturezaCarga(1400); 
            }        if (model.getIdEmbalagem() == null) log.error("DTM {}: ID da Embalagem é obrigatório e não foi encontrado.", model.getIdDtm());

        if (model.getHrColetaFim() == null || model.getHrColetaFim().isBlank()) {
            model.setHrColetaFim(appProperties.getDefaults().getHrFim());
        }
        if (model.getTpModal() == null) {
            model.setTpModal(appProperties.getDefaults().getModal() == AppProperties.Defaults.Modal.AEREO ? 2 : 1);
        }
    }
}