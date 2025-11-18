package com.tecnolog.autocoleta.dtm;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime; 
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.tecnolog.autocoleta.config.AppProperties;
import com.tecnolog.autocoleta.domain.Modal;
import com.tecnolog.autocoleta.dto.salvarcoleta.SalvaColetaModel;

@Repository
public class SqlServerRepository {

    private static final Logger log = LoggerFactory.getLogger(SqlServerRepository.class);
    private final JdbcTemplate jdbc;
    private final AppProperties appProperties;
    private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("[^\\d]");
    private static final ZoneId SAO_PAULO_ZONE_ID = ZoneId.of("America/Sao_Paulo");

    private List<NaturezaMapping> cacheMapeamentoNatureza = Collections.emptyList();
    private record NaturezaMapping(String keywordUpper, int targetId) {}

    private List<CidadeAgenteMapping> cacheMapeamentoCidadeAgente = Collections.emptyList();
    private record CidadeAgenteMapping(String chaveCidadeUpper, int targetAgenteId) {}

    private Map<String, String> cachePessoaAlias = Collections.emptyMap();

    private List<EmbalagemMapping> cacheMapeamentoEmbalagem = Collections.emptyList();
    private record EmbalagemMapping(String keywordUpper, int targetId) {}


    public SqlServerRepository(@Qualifier("sqlServerJdbcTemplate") JdbcTemplate jdbc, AppProperties appProperties) {
        this.jdbc = jdbc;
        this.appProperties = appProperties;
        
        carregarCacheMapeamentoNatureza();
        carregarCacheMapeamentoCidadeAgente();
        carregarCachePessoaAlias();
        carregarCacheMapeamentoEmbalagem();
    }

    private void carregarCacheMapeamentoNatureza() {
        String sql = "SELECT ds_PalavraChave, id_NaturezaMercadoria_Alvo FROM tbdMapeamentoNatureza ORDER BY nr_Prioridade ASC";
        try {
            this.cacheMapeamentoNatureza = jdbc.query(sql, (rs, rowNum) -> 
                new NaturezaMapping(
                    rs.getString("ds_PalavraChave").toUpperCase(), 
                    rs.getInt("id_NaturezaMercadoria_Alvo")
                )
            );
            log.info("Cache de Mapeamento de Natureza carregado com {} regras.", this.cacheMapeamentoNatureza.size());
        } catch (Exception e) {
            log.error("FALHA CRÍTICA AO CARREGAR CACHE DE MAPEAMENTO DE NATUREZA.", e);
        }
    }

    private void carregarCacheMapeamentoCidadeAgente() {
        String sql = "SELECT ds_ChaveCidade, id_Pessoa_Agente_Alvo FROM tbdMapeamentoCidadeAgente";
        try {
            this.cacheMapeamentoCidadeAgente = jdbc.query(sql, (rs, rowNum) -> 
                new CidadeAgenteMapping(
                    rs.getString("ds_ChaveCidade").toUpperCase(),
                    rs.getInt("id_Pessoa_Agente_Alvo")
                )
            );
            log.info("Cache de Mapeamento de Cidade/Agente carregado com {} regras.", this.cacheMapeamentoCidadeAgente.size());
        } catch (Exception e) {
            log.error("FALHA CRÍTICA AO CARREGAR CACHE DE MAPEAMENTO DE CIDADE/AGENTE.", e);
        }
    }

    private void carregarCachePessoaAlias() {
        String sql = "SELECT ds_Alias, ds_NomeAlvo FROM tbdMapeamentoPessoaAlias";
        try {
            this.cachePessoaAlias = jdbc.query(sql, (rs, rowNum) -> 
                Map.entry(
                    rs.getString("ds_Alias").toUpperCase(),
                    rs.getString("ds_NomeAlvo")
                )
            ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            
            log.info("Cache de Mapeamento de Apelidos (Pessoa Alias) carregado com {} regras.", this.cachePessoaAlias.size());
        } catch (Exception e) {
            log.error("FALHA CRÍTICA AO CARREGAR CACHE DE MAPEAMENTO DE APELIDOS (PESSOA ALIAS).", e);
        }
    }

    private void carregarCacheMapeamentoEmbalagem() {
        String sql = "SELECT ds_PalavraChave, id_Embalagem_Alvo FROM tbdMapeamentoEmbalagem ORDER BY nr_Prioridade ASC";
        try {
            this.cacheMapeamentoEmbalagem = jdbc.query(sql, (rs, rowNum) -> 
                new EmbalagemMapping(
                    rs.getString("ds_PalavraChave").toUpperCase(), 
                    rs.getInt("id_Embalagem_Alvo")
                )
            );
            log.info("Cache de Mapeamento de Embalagem carregado com {} regras.", this.cacheMapeamentoEmbalagem.size());
        } catch (Exception e) {
            log.error("FALHA CRÍTICA AO CARREGAR CACHE DE MAPEAMENTO DE EMBALAGEM.", e);
        }
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

        if (model.getIdEnderecoCidade() == null && model.getDsCidadeColeta() != null && !model.getDsCidadeColeta().isBlank()) {
             model.setIdEnderecoCidade(findCidadeIdByName(model.getDsCidadeColeta()));
        }

        Integer agenteId = findAgenteIdByNomeOuEmail(model.getDsAgenteNome(), model.getDsAgenteEmail());

        if (agenteId == null && model.getDsCidadeColeta() != null && !model.getDsCidadeColeta().isBlank()) {
            log.info("DTM {}: Agente não encontrado pelo nome ({}). Tentando busca pela cidade de coleta ({})",
                    model.getIdDtm(), model.getDsAgenteNome(), model.getDsCidadeColeta());

            Integer agenteIdDoMapa = findAgenteIdByCidadeColeta(model.getDsCidadeColeta());

            if (agenteIdDoMapa != null) {
                log.info("DTM {}: Cidade de coleta ({}) mapeada DIRETAMENTE para o agente ID {}.",
                        model.getIdDtm(), model.getDsCidadeColeta(), agenteIdDoMapa);
                agenteId = agenteIdDoMapa;
            } else {
                log.warn("DTM {}: Nenhum agente encontrado no mapa (cache DB) para a cidade '{}'",
                        model.getIdDtm(), model.getDsCidadeColeta());
            }
        }
        
        model.setIdAgente(agenteId); 
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
            log.debug("Nenhum nome ou email fornecido para buscar o Agente.");
            return null;
        }

        String siglaBusca = null;
        String nomeCompleto = nome;
        String nomeApenas = nome;     

        if (nome != null && nome.contains(" - ")) {
            try {
                String[] parts = nome.split(" - ", 2);
                siglaBusca = parts[0].trim();
                nomeApenas = parts[1].trim();
                log.debug("Input do Agente '{}' foi dividido em Sigla/Cidade/UF '{}' e Nome '{}'", nomeCompleto, siglaBusca, nomeApenas);
            } catch (Exception e) {
                log.warn("Falha ao tentar dividir o nome do agente '{}'. Usando o nome completo para todas as buscas.", nomeCompleto);
                siglaBusca = null;
                nomeApenas = nomeCompleto; 
            }
        }
        
        List<Object> params = new ArrayList<>();

        if (siglaBusca != null && !siglaBusca.isBlank() && nomeCompleto != null && !nomeCompleto.isBlank()) {
            String sql1 = "SELECT TOP 1 p.id_Pessoa FROM tbdPessoa p " + 
                          "LEFT JOIN tbdCidade c ON p.id_Cidade = c.id_Cidade " +
                          "WHERE (LOWER(c.cd_Sigla) = ? AND RTRIM(LOWER(p.ds_Pessoa)) = ?)";
                          
            params.add(siglaBusca.toLowerCase());
            params.add(nomeCompleto.toLowerCase());
            
            try {
                Integer id = jdbc.queryForObject(sql1, Integer.class, params.toArray());
                log.info("ID do Agente (Pessoa) encontrado com busca [Combinada Sigla+Nome Exato]: {} (busca por nome='{}', sigla='{}')", id, nomeCompleto, siglaBusca);
                return id;
            } catch (EmptyResultDataAccessException e) {
                log.debug("Nenhum Agente (Pessoa) encontrado com busca [Combinada Sigla+Nome Exato]. Tentando próxima.");
            } catch (Exception e) {
                log.error("Erro ao executar busca [Combinada Sigla+Nome Exato] de Agente: {}", e.getMessage(), e);
            }
        }

        if (nomeCompleto != null && !nomeCompleto.isBlank()) {
             params.clear();
             String sql2 = "SELECT TOP 1 p.id_Pessoa FROM tbdPessoa p WHERE (RTRIM(LOWER(p.ds_Pessoa)) = ?)";
             params.add(nomeCompleto.toLowerCase());
             
             try {
                Integer id = jdbc.queryForObject(sql2, Integer.class, params.toArray());
                log.info("ID do Agente (Pessoa) encontrado com busca [Nome Completo Exato]: {} (busca por nome='{}')", id, nomeCompleto);
                return id;
            } catch (EmptyResultDataAccessException e) {
                log.debug("Nenhum Agente (Pessoa) encontrado com busca [Nome Completo Exato]. Tentando próxima.");
            } catch (Exception e) {
                log.error("Erro ao executar busca [Nome Completo Exato] de Agente: {}", e.getMessage(), e);
            }
        }
        
        if (email != null && !email.isBlank()) {
             params.clear();
             String sql3 = "SELECT TOP 1 p.id_Pessoa FROM tbdPessoa p WHERE (LOWER(p.cd_Email) = ?)";
             params.add(email.toLowerCase());
             
             try {
                Integer id = jdbc.queryForObject(sql3, Integer.class, params.toArray());
                log.info("ID do Agente (Pessoa) encontrado com busca [Email]: {} (busca por email='{}')", id, email);
                return id;
            } catch (EmptyResultDataAccessException e) {
                log.debug("Nenhum Agente (Pessoa) encontrado com busca [Email]. Tentando próxima.");
            } catch (Exception e) {
                log.error("Erro ao executar busca [Email] de Agente: {}", e.getMessage(), e);
            }
        }

        log.warn("Nenhum Agente (Pessoa) encontrado com busca específica para nome='{}', email='{}', sigla='{}'", nomeCompleto, email, siglaBusca);
        return null;
    }

    private Integer findPessoaId(String nome, String cnpj) {
        if ((nome == null || nome.isBlank()) && (cnpj == null || cnpj.isBlank())) {
            return null;
        }

        if (nome != null && !nome.isBlank()) {
            String nomeBusca = nome;
            String nomeUpper = nome.toUpperCase();

            if (cachePessoaAlias.containsKey(nomeUpper)) {
                nomeBusca = cachePessoaAlias.get(nomeUpper);
                log.info("Apelido de Pessoa (do cache DB) '{}' traduzido para busca como '{}'", nome, nomeBusca);
            }

            String sqlNome = "SELECT TOP 1 id_Pessoa FROM tbdPessoa WHERE (LOWER(ds_Pessoa) COLLATE Latin1_General_CI_AI LIKE ? OR LOWER(ds_RazaoSocial) COLLATE Latin1_General_CI_AI LIKE ?)";
            List<Object> params = new ArrayList<>();
            params.add("%" + nomeBusca.toLowerCase() + "%");
            params.add("%" + nomeBusca.toLowerCase() + "%");

            try {
                Integer id = jdbc.queryForObject(sqlNome, Integer.class, params.toArray());
                log.info("ID de Pessoa encontrado via Nome/Apelido: {} (para nome='{}')", id, nome);
                return id;
            } catch (EmptyResultDataAccessException e) {
                log.warn("Nenhuma Pessoa encontrada para o Nome/Apelido '{}'. Prosseguindo para buscar pelo CNPJ.", nome);
            }
        }

        if (cnpj != null && !cnpj.isBlank()) {
            try {
                String cleanCnpj = NON_DIGIT_PATTERN.matcher(cnpj).replaceAll("");
                String sqlCnpj = "SELECT TOP 1 id_Pessoa FROM tbdPessoa WHERE cd_CGCCPF = ?";
                Integer id = jdbc.queryForObject(sqlCnpj, Integer.class, cleanCnpj);
                log.info("ID de Pessoa encontrado via CNPJ: {} (para cnpj='{}')", id, cnpj);
                return id;
            } catch (EmptyResultDataAccessException e) {
                log.warn("Nenhuma Pessoa encontrada para o CNPJ '{}' (após falha na busca por nome, se aplicável).", cnpj);
            }
        }

        log.error("Não foi possível encontrar um ID de Pessoa para nome='{}' ou cnpj='{}'", nome, cnpj);
        return null;
    }

    private Integer findEmbalagemIdComDePara(String nomeOrigem) {
        if (nomeOrigem == null || nomeOrigem.isBlank()) return null;

        String nomeOrigemUpper = nomeOrigem.toUpperCase();
        
        for (EmbalagemMapping mapping : this.cacheMapeamentoEmbalagem) {
            if (nomeOrigemUpper.contains(mapping.keywordUpper())) {
                log.info("Mapeamento de embalagem (do cache DB) encontrado: '{}' -> ID {}", nomeOrigem, mapping.targetId());
                return mapping.targetId();
            }
        }

        log.warn("Não foi encontrado mapeamento de keyword (do cache DB) para a embalagem '{}'. Tentando busca direta por LIKE.", nomeOrigem);
        try {
            String sql = "SELECT TOP 1 id_Embalagem FROM tbdEmbalagem WHERE LOWER(ds_Embalagem) COLLATE Latin1_General_CI_AI LIKE ?";
            return jdbc.queryForObject(sql, Integer.class, "%" + nomeOrigem.toLowerCase() + "%");
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a Embalagem contendo '{}' (nem no cache, nem por LIKE).", nomeOrigem);
            return null;
        } catch (Exception e) {
            log.error("Erro ao buscar ID de Embalagem para '{}': {}", nomeOrigem, e.getMessage(), e);
            return null;
        }
    }

    
    private Integer findNaturezaIdComDePara(String nomeOrigem) {
        if (nomeOrigem == null || nomeOrigem.isBlank()) return null;

        String nomeOrigemUpper = nomeOrigem.toUpperCase();

        for (NaturezaMapping mapping : this.cacheMapeamentoNatureza) {
            if (nomeOrigemUpper.contains(mapping.keywordUpper())) {
                log.info("Mapeamento de natureza (do cache DB) encontrado: '{}' -> ID {}", nomeOrigem, mapping.targetId());
                return mapping.targetId();
            }
        }

        log.warn("Não foi encontrado mapeamento de keyword (do cache DB) para a natureza '{}'. Tentando busca direta por LIKE.", nomeOrigem);
        try {
            String sql = "SELECT TOP 1 id_NaturezaMercadoria FROM tbdNaturezaMercadoria WHERE LOWER(ds_NaturezaMercadoria) COLLATE Latin1_General_CI_AI LIKE ?";
            return jdbc.queryForObject(sql, Integer.class, "%" + nomeOrigem.toLowerCase() + "%");
        } catch (EmptyResultDataAccessException e) {
            log.warn("Não foi possível encontrar um ID para a Natureza da Carga contendo '{}' (nem no cache, nem por LIKE).", nomeOrigem);
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

    private Integer findCidadeIdByName(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return null;
        }
        
        String trimmedCityName = cityName.trim();
        if (trimmedCityName.contains("/")) {
            trimmedCityName = trimmedCityName.split("/")[0].trim();
        }

        if (trimmedCityName.isEmpty()) {
            return null;
        }

        String sql = "SELECT TOP 1 id_Cidade FROM tbdCidade WHERE LOWER(ds_Cidade) COLLATE Latin1_General_CI_AI LIKE ?";
        try {
            Integer id = jdbc.queryForObject(sql, Integer.class, trimmedCityName.toLowerCase() + "%");
            log.info("ID de Cidade encontrado via Nome: {} (para nome='{}')", id, cityName);
            return id;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Nenhuma Cidade encontrada para o nome '{}' na tbdCidade.", cityName);
            return null;
        } catch (Exception e) {
            log.error("Erro ao buscar ID de Cidade pelo nome '{}' na tbdCidade: {}", cityName, e.getMessage(), e);
            return null;
        }
    }

    private void fillDefaultsIfNull(SalvaColetaModel model) {
        if (model.getIdRemetente() == null) model.setIdRemetente(appProperties.getDefaults().getIdRemetente());
        if (model.getIdDestinatario() == null) model.setIdDestinatario(appProperties.getDefaults().getIdDestinatario());
        if (model.getIdTomador() == null) model.setIdTomador(appProperties.getDefaults().getIdTomador());
        if (model.getIdFilialResposavel() == null) model.setIdFilialResposavel(appProperties.getDefaults().getIdFilialResposavel());
        if (model.getIdLocalColeta() == null) model.setIdLocalColeta(appProperties.getDefaults().getIdLocalColeta());
        
        if (model.getIdTipoColeta() == null) model.setIdTipoColeta(appProperties.getDefaults().getIdTipoColetaDefault());
        if (model.getIdAgente() == null) {
            log.warn("DTM {}: Agente não encontrado por nome nem por cidade de coleta. Aplicando agente padrão (ID: {}).", model.getIdDtm(), appProperties.getDefaults().getIdAgente());
            model.setIdAgente(appProperties.getDefaults().getIdAgente());
        }

        if (model.getIdNaturezaCarga() == null) {
             log.warn("DTM {}: ID da Natureza da Carga não foi encontrado. Aplicando natureza genérica de fallback (ID: 1345).", model.getIdDtm());
             model.setIdNaturezaCarga(1345); 
        }
        if (model.getIdEmbalagem() == null) {
            log.error("DTM {}: ID da Embalagem é obrigatório e não foi encontrado. Aplicando embalagem de fallback (ID: 33).", model.getIdDtm());
            model.setIdEmbalagem(33);
        }

        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String tipoColeta = model.getDsTipoColeta();
        LocalDate dtColeta = model.getDtColeta();
        LocalDate hoje = LocalDate.now(SAO_PAULO_ZONE_ID);
        
        boolean isEmergencia = tipoColeta != null && tipoColeta.toUpperCase().contains("EMERGÊNCIA");
        
        if (isEmergencia && dtColeta != null && dtColeta.isEqual(hoje)) {
            LocalTime agora = LocalTime.now(SAO_PAULO_ZONE_ID);
            String hrInicioAgora = agora.format(formatter);
            String hrFimAgoraMais6 = agora.plusHours(6).format(formatter);
            
            model.setHrColetaInicio(hrInicioAgora);
            model.setHrColetaFim(hrFimAgoraMais6);
            
            log.info("DTM {}: REGRA EMERGÊNCIA (MESMO DIA) aplicada. Início: {}, Fim: {}", 
                      model.getIdDtm(), hrInicioAgora, hrFimAgoraMais6);
                      
        } else {
            
            if (model.getHrColetaInicio() == null || model.getHrColetaInicio().isBlank()) {
                model.setHrColetaInicio("08:00");
                log.warn("DTM {}: HrColetaInicio veio NULA da View. Aplicando padrão: 08:00", model.getIdDtm());
            }
            
            if (model.getHrColetaFim() == null || model.getHrColetaFim().isBlank()) {
                 if (isEmergencia) {
                    model.setHrColetaFim("14:00");
                    log.warn("DTM {}: HrColetaFim (Emergência Futura) veio NULA da View. Aplicando padrão: 14:00", model.getIdDtm());
                 } else {
                    model.setHrColetaFim("16:00");
                    log.warn("DTM {}: HrColetaFim (Normal) veio NULA da View. Aplicando padrão: 16:00", model.getIdDtm());
                 }
            }
    
            if (isEmergencia) {
                log.info("DTM {}: REGRA EMERGÊNCIA (DATA FUTURA) aplicada. Data: {}. Horário: {}-{}", 
                         model.getIdDtm(), dtColeta, model.getHrColetaInicio(), model.getHrColetaFim());
            } else {
                log.debug("DTM {}: Horário NORMAL (vinda View) aplicado. Data: {}. Horário: {}-{}",
                         model.getIdDtm(), dtColeta, model.getHrColetaInicio(), model.getHrColetaFim());
            }
        }
        
        if (model.getTpModal() == null) {
            AppProperties.Defaults.Modal defaultModalEnum = appProperties.getDefaults().getModal();
            if (defaultModalEnum != null) {
                 try {
                     model.setTpModal(Modal.valueOf(defaultModalEnum.name()));
                     log.debug("DTM {}: TpModal definido para o padrão: {}", model.getIdDtm(), model.getTpModal());
                 } catch (IllegalArgumentException e) {
                      log.error("DTM {}: Enum Modal '{}' definido em AppProperties não corresponde ao enum Modal do domínio. Usando AÉREO como fallback.", model.getIdDtm(), defaultModalEnum.name());
                      model.setTpModal(Modal.AEREO);
                 }
            } else {
                 log.warn("DTM {}: Modal padrão não definido em AppProperties. Usando AÉREO como fallback.", model.getIdDtm());
                 model.setTpModal(Modal.AEREO);
            }
        }
    }
    
    private Integer findAgenteIdByCidadeColeta(String cidadeColeta) {
        String cidadeNormalizada = normalizarString(cidadeColeta);
        if (cidadeNormalizada == null) {
            return null;
        }
        
        for (CidadeAgenteMapping mapping : this.cacheMapeamentoCidadeAgente) {
            if (mapping.chaveCidadeUpper().equals(cidadeNormalizada)) {
                log.debug("Mapeamento de agente por cidade (cache DB, chave exata) encontrado: '{}' -> ID {}", cidadeNormalizada, mapping.targetAgenteId());
                return mapping.targetAgenteId();
            }
        }

        if (!cidadeNormalizada.contains("/")) {
            String cidadeComBarra = cidadeNormalizada + "/";
            for (CidadeAgenteMapping mapping : this.cacheMapeamentoCidadeAgente) {
                if (mapping.chaveCidadeUpper().startsWith(cidadeComBarra)) {
                    log.info("Mapeamento de agente por cidade (cache DB, parcial) encontrado: Cidade '{}' corresponde à chave '{}' do mapa.", cidadeNormalizada, mapping.chaveCidadeUpper());
                    return mapping.targetAgenteId(); 
                }
            }
        }
        
        for (CidadeAgenteMapping mapping : this.cacheMapeamentoCidadeAgente) {
             if (mapping.chaveCidadeUpper().contains(cidadeNormalizada)) {
                log.warn("Mapeamento de agente por cidade (cache DB, 'contains', menos preciso) encontrado: Cidade '{}' corresponde à chave '{}' do mapa.", cidadeNormalizada, mapping.chaveCidadeUpper());
                return mapping.targetAgenteId();
             }
        }

        return null;
    }

    private String normalizarString(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        
        return normalized.toUpperCase().trim();
    }
}