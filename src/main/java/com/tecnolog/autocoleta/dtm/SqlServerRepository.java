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
            this.cacheMapeamentoNatureza = jdbc.query(sql, (rs, rowNum) -> new NaturezaMapping(rs.getString("ds_PalavraChave").toUpperCase(), rs.getInt("id_NaturezaMercadoria_Alvo")));
        } catch (Exception e) { log.error("Erro cache natureza", e); }
    }

    private void carregarCacheMapeamentoCidadeAgente() {
        String sql = "SELECT ds_ChaveCidade, id_Pessoa_Agente_Alvo FROM tbdMapeamentoCidadeAgente";
        try {
            this.cacheMapeamentoCidadeAgente = jdbc.query(sql, (rs, rowNum) -> new CidadeAgenteMapping(rs.getString("ds_ChaveCidade").toUpperCase(), rs.getInt("id_Pessoa_Agente_Alvo")));
        } catch (Exception e) { log.error("Erro cache cidade agente", e); }
    }

    private void carregarCachePessoaAlias() {
        String sql = "SELECT ds_Alias, ds_NomeAlvo FROM tbdMapeamentoPessoaAlias";
        try {
            this.cachePessoaAlias = jdbc.query(sql, (rs, rowNum) -> Map.entry(rs.getString("ds_Alias").toUpperCase(), rs.getString("ds_NomeAlvo"))).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) { log.error("Erro cache alias", e); }
    }

    private void carregarCacheMapeamentoEmbalagem() {
        String sql = "SELECT ds_PalavraChave, id_Embalagem_Alvo FROM tbdMapeamentoEmbalagem ORDER BY nr_Prioridade ASC";
        try {
            this.cacheMapeamentoEmbalagem = jdbc.query(sql, (rs, rowNum) -> new EmbalagemMapping(rs.getString("ds_PalavraChave").toUpperCase(), rs.getInt("id_Embalagem_Alvo")));
        } catch (Exception e) { log.error("Erro cache embalagem", e); }
    }

    public Integer findExistingColetaIdByDtm(String dtmId) {
        if (dtmId == null || dtmId.isBlank()) return null;
        String sql = "SELECT TOP 1 pc.id_PedidoColeta FROM tbdPedidoColeta pc LEFT JOIN tbdItemPedidoColeta ipc ON pc.id_PedidoColeta = ipc.id_PedidoColeta WHERE ipc.nr_Referencia = ? OR ipc.nr_PedidoCliente = ? OR pc.cm_PedidoColeta LIKE ?";
        try {
            return jdbc.queryForObject(sql, Integer.class, dtmId, dtmId, "%" + dtmId + "%");
        } catch (EmptyResultDataAccessException e) { return null; }
    }

    public void preencherDadosFaltantes(SalvaColetaModel model) {

        model.setIdRemetente(findPessoaId(model.getDsRemetente(), model.getCdRemetenteCnpj()));
        model.setIdTomador(findPessoaId(model.getDsTomador(), model.getCdTomadorCnpj()));

        Integer idDestinatario = findPessoaIdRestritoCidade(
            model.getDsDestinatario(), 
            model.getCdDestinatarioCnpj(), 
            model.getDsCidadeDestino()
        );
        
        if (idDestinatario != null) {
            model.setIdDestinatario(idDestinatario);
        } else {
            log.warn("DTM {}: Destinatário não encontrado na cidade '{}'. Tentando busca genérica...", model.getIdDtm(), model.getDsCidadeDestino());
            model.setIdDestinatario(findPessoaId(model.getDsDestinatario(), model.getCdDestinatarioCnpj()));
        }

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
                agenteId = agenteIdDoMapa;
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
            } catch (EmptyResultDataAccessException e) { }
        }
        
        fillDefaultsIfNull(model);
    }

    private Integer findPessoaIdRestritoCidade(String nome, String cnpj, String cidadeInput) {
        if (cidadeInput == null || cidadeInput.isBlank()) {
            log.warn("Cidade destino vazia. Usando busca padrão de pessoa.");
            return findPessoaId(nome, cnpj);
        }

        String cidadeLimpa = cidadeInput.split("/")[0].trim();
        
        if (cnpj != null && !cnpj.isBlank()) {
            String cleanCnpj = NON_DIGIT_PATTERN.matcher(cnpj).replaceAll("");
            String sql = """
                SELECT TOP 1 p.id_Pessoa 
                FROM tbdPessoa p
                JOIN tbdCidade c ON p.id_Cidade = c.id_Cidade
                WHERE p.cd_CGCCPF = ? 
                AND LOWER(c.ds_Cidade) COLLATE Latin1_General_CI_AI LIKE ?
            """;
            try {
                Integer id = jdbc.queryForObject(sql, Integer.class, cleanCnpj, cidadeLimpa.toLowerCase());
                log.info("Destinatário encontrado por CNPJ '{}' na cidade '{}': ID {}", cnpj, cidadeLimpa, id);
                return id;
            } catch (EmptyResultDataAccessException e) {
                log.debug("CNPJ match falhou na cidade '{}'. Tentando por nome...", cidadeLimpa);
            }
        }

        if (nome != null && !nome.isBlank()) {
            String nomeBusca = nome;
            if (cachePessoaAlias.containsKey(nome.toUpperCase())) {
                nomeBusca = cachePessoaAlias.get(nome.toUpperCase());
            }

            String sql = """
                SELECT TOP 1 p.id_Pessoa 
                FROM tbdPessoa p
                JOIN tbdCidade c ON p.id_Cidade = c.id_Cidade
                WHERE (LOWER(p.ds_Pessoa) COLLATE Latin1_General_CI_AI LIKE ? OR LOWER(p.ds_RazaoSocial) COLLATE Latin1_General_CI_AI LIKE ?)
                AND LOWER(c.ds_Cidade) COLLATE Latin1_General_CI_AI LIKE ?
            """;
            try {
                String likeNome = "%" + nomeBusca.toLowerCase() + "%";
                String likeCidade = cidadeLimpa.toLowerCase() + "%"; // Like na cidade para pegar variações pequenas
                
                Integer id = jdbc.queryForObject(sql, Integer.class, likeNome, likeNome, likeCidade);
                log.info("Destinatário encontrado por Nome '{}' na cidade '{}': ID {}", nome, cidadeLimpa, id);
                return id;
            } catch (EmptyResultDataAccessException e) {
                log.warn("Destinatário '{}' não encontrado especificamente na cidade '{}'.", nome, cidadeLimpa);
            }
        }

        return null;
    }

    private Integer findAgenteIdByNomeOuEmail(String nome, String email) {
        if ((nome == null || nome.isBlank()) && (email == null || email.isBlank())) return null;
        String siglaBusca = null; String nomeCompleto = nome;
        if (nome != null && nome.contains(" - ")) {
            try { String[] parts = nome.split(" - ", 2); siglaBusca = parts[0].trim(); nomeCompleto = parts[1].trim(); } catch (Exception e) {}
        }
        List<Object> params = new ArrayList<>();
        if (siglaBusca != null && !siglaBusca.isBlank()) {
            String sql1 = "SELECT TOP 1 p.id_Pessoa FROM tbdPessoa p LEFT JOIN tbdCidade c ON p.id_Cidade = c.id_Cidade WHERE (LOWER(c.cd_Sigla) = ? AND RTRIM(LOWER(p.ds_Pessoa)) = ?)";
            try { return jdbc.queryForObject(sql1, Integer.class, siglaBusca.toLowerCase(), nomeCompleto.toLowerCase()); } catch (Exception e) {}
        }
        if (nomeCompleto != null && !nomeCompleto.isBlank()) {
            String sql2 = "SELECT TOP 1 p.id_Pessoa FROM tbdPessoa p WHERE (RTRIM(LOWER(p.ds_Pessoa)) = ?)";
            try { return jdbc.queryForObject(sql2, Integer.class, nomeCompleto.toLowerCase()); } catch (Exception e) {}
        }
        if (email != null && !email.isBlank()) {
            String sql3 = "SELECT TOP 1 p.id_Pessoa FROM tbdPessoa p WHERE (LOWER(p.cd_Email) = ?)";
            try { return jdbc.queryForObject(sql3, Integer.class, email.toLowerCase()); } catch (Exception e) {}
        }
        return null;
    }

    private Integer findPessoaId(String nome, String cnpj) {
        if ((nome == null || nome.isBlank()) && (cnpj == null || cnpj.isBlank())) return null;
        if (cnpj != null && !cnpj.isBlank()) {
            try {
                String cleanCnpj = NON_DIGIT_PATTERN.matcher(cnpj).replaceAll("");
                String sqlCnpj = "SELECT TOP 1 id_Pessoa FROM tbdPessoa WHERE cd_CGCCPF = ?";
                return jdbc.queryForObject(sqlCnpj, Integer.class, cleanCnpj);
            } catch (Exception e) { }
        }
        if (nome != null && !nome.isBlank()) {
            String nomeBusca = nome;
            if (cachePessoaAlias.containsKey(nome.toUpperCase())) nomeBusca = cachePessoaAlias.get(nome.toUpperCase());
            String sqlNome = "SELECT TOP 1 id_Pessoa FROM tbdPessoa WHERE (LOWER(ds_Pessoa) COLLATE Latin1_General_CI_AI LIKE ? OR LOWER(ds_RazaoSocial) COLLATE Latin1_General_CI_AI LIKE ?)";
            try {
                return jdbc.queryForObject(sqlNome, Integer.class, "%" + nomeBusca.toLowerCase() + "%", "%" + nomeBusca.toLowerCase() + "%");
            } catch (Exception e) { }
        }
        return null;
    }

    private Integer findEmbalagemIdComDePara(String nomeOrigem) {
        if (nomeOrigem == null || nomeOrigem.isBlank()) return null;
        String nomeUpper = nomeOrigem.toUpperCase();
        for (EmbalagemMapping mapping : this.cacheMapeamentoEmbalagem) {
            if (nomeUpper.contains(mapping.keywordUpper())) return mapping.targetId();
        }
        try {
            String sql = "SELECT TOP 1 id_Embalagem FROM tbdEmbalagem WHERE LOWER(ds_Embalagem) COLLATE Latin1_General_CI_AI LIKE ?";
            return jdbc.queryForObject(sql, Integer.class, "%" + nomeOrigem.toLowerCase() + "%");
        } catch (Exception e) { return null; }
    }
    
    private Integer findNaturezaIdComDePara(String nomeOrigem) {
        if (nomeOrigem == null || nomeOrigem.isBlank()) return null;
        String nomeUpper = nomeOrigem.toUpperCase();
        for (NaturezaMapping mapping : this.cacheMapeamentoNatureza) {
            if (nomeUpper.contains(mapping.keywordUpper())) return mapping.targetId();
        }
        try {
            String sql = "SELECT TOP 1 id_NaturezaMercadoria FROM tbdNaturezaMercadoria WHERE LOWER(ds_NaturezaMercadoria) COLLATE Latin1_General_CI_AI LIKE ?";
            return jdbc.queryForObject(sql, Integer.class, "%" + nomeOrigem.toLowerCase() + "%");
        } catch (Exception e) { return null; }
    }

    private Integer findTipoColetaIdByName(String nome) {
        if (nome == null || nome.isBlank()) return null;
        String termoBusca = nome.toUpperCase();
        if (termoBusca.endsWith("O") || termoBusca.endsWith("A")) termoBusca = termoBusca.substring(0, termoBusca.length() - 1);
        try {
            String sql = "SELECT TOP 1 id_TipoPedidoColeta FROM tbdTipoPedidoColeta WHERE LOWER(ds_TipoPedidoColeta) COLLATE Latin1_General_CI_AI LIKE ?";
            return jdbc.queryForObject(sql, Integer.class, termoBusca.toLowerCase() + "%");
        } catch (Exception e) { return null; }
    }

    private Integer findCidadeIdByName(String cityName) {
        if (cityName == null || cityName.isBlank()) return null;
        String trimmedCityName = cityName.trim();
        if (trimmedCityName.contains("/")) trimmedCityName = trimmedCityName.split("/")[0].trim();
        String sql = "SELECT TOP 1 id_Cidade FROM tbdCidade WHERE LOWER(ds_Cidade) COLLATE Latin1_General_CI_AI LIKE ?";
        try {
            return jdbc.queryForObject(sql, Integer.class, trimmedCityName.toLowerCase() + "%");
        } catch (Exception e) { return null; }
    }

    private void fillDefaultsIfNull(SalvaColetaModel model) {
        if (model.getIdRemetente() == null) model.setIdRemetente(appProperties.getDefaults().getIdRemetente());
        if (model.getIdDestinatario() == null) model.setIdDestinatario(appProperties.getDefaults().getIdDestinatario());
        if (model.getIdTomador() == null) model.setIdTomador(appProperties.getDefaults().getIdTomador());
        if (model.getIdFilialResposavel() == null) model.setIdFilialResposavel(appProperties.getDefaults().getIdFilialResposavel());
        if (model.getIdLocalColeta() == null) model.setIdLocalColeta(appProperties.getDefaults().getIdLocalColeta());
        if (model.getIdTipoColeta() == null) model.setIdTipoColeta(appProperties.getDefaults().getIdTipoColetaDefault());
        
        if (model.getIdAgente() == null) {
            model.setIdAgente(appProperties.getDefaults().getIdAgente());
        }

        if (model.getIdNaturezaCarga() == null) model.setIdNaturezaCarga(1345); 
        if (model.getIdEmbalagem() == null) model.setIdEmbalagem(33);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String tipoColeta = model.getDsTipoColeta();
        LocalDate dtColeta = model.getDtColeta();
        LocalDate hoje = LocalDate.now(SAO_PAULO_ZONE_ID);
        
        boolean isEmergencia = tipoColeta != null && tipoColeta.toUpperCase().contains("EMERGÊNCIA");
        
        if (isEmergencia && dtColeta != null && dtColeta.isEqual(hoje)) {
            LocalTime agora = LocalTime.now(SAO_PAULO_ZONE_ID);
            model.setHrColetaInicio(agora.format(formatter));
            model.setHrColetaFim(agora.plusHours(6).format(formatter));
        } else {
            if (model.getHrColetaInicio() == null || model.getHrColetaInicio().isBlank()) model.setHrColetaInicio("08:00");
            if (model.getHrColetaFim() == null || model.getHrColetaFim().isBlank()) {
                 if (isEmergencia) model.setHrColetaFim("14:00");
                 else model.setHrColetaFim("16:00");
            }
        }
        
        if (model.getTpModal() == null) {
            AppProperties.Defaults.Modal defaultModalEnum = appProperties.getDefaults().getModal();
            if (defaultModalEnum != null) {
                 try { model.setTpModal(Modal.valueOf(defaultModalEnum.name())); } 
                 catch (IllegalArgumentException e) { model.setTpModal(Modal.AEREO); }
            } else { model.setTpModal(Modal.AEREO); }
        }
    }
    
    private Integer findAgenteIdByCidadeColeta(String cidadeColeta) {
        String cidadeNormalizada = normalizarString(cidadeColeta);
        if (cidadeNormalizada == null) return null;
        for (CidadeAgenteMapping mapping : this.cacheMapeamentoCidadeAgente) {
            if (mapping.chaveCidadeUpper().equals(cidadeNormalizada)) return mapping.targetAgenteId();
        }
        if (!cidadeNormalizada.contains("/")) {
            String cidadeComBarra = cidadeNormalizada + "/";
            for (CidadeAgenteMapping mapping : this.cacheMapeamentoCidadeAgente) {
                if (mapping.chaveCidadeUpper().startsWith(cidadeComBarra)) return mapping.targetAgenteId(); 
            }
        }
        for (CidadeAgenteMapping mapping : this.cacheMapeamentoCidadeAgente) {
             if (mapping.chaveCidadeUpper().contains(cidadeNormalizada)) return mapping.targetAgenteId();
        }
        return null;
    }

    private String normalizarString(String input) {
        if (input == null || input.isBlank()) return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return normalized.toUpperCase().trim();
    }
}