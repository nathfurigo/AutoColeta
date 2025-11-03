package com.tecnolog.autocoleta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaixarXmlCte {

    private static final String SQLSERVER_SERVER = "192.168.10.251";
    private static final String SQLSERVER_PORT = "1433";
    private static final String SQLSERVER_USER = "sa";
    private static final String SQLSERVER_PASSWORD = "esl@13509";

    private static final List<String> DATABASES_TO_SEARCH = Arrays.asList(
            "dtbCTe2024",
            "dtbCTe2025"
    );

    private static final String PASTA_RAIZ_DOWNLOAD = "C:\\XMLs_Busca_Simples\\";

    private static final Map<String, String> DTM_PARA_CHAVE_MAP = new HashMap<>();

    private static class DtmInfo {
        String dtm;
        int mes;
        int ano;
        String dbName; // ex: dtbCTe2025
        String tableName; // ex: [dbo].[tbdCTeXMLMovimento10]

        DtmInfo(String dtm, int mes, int ano) {
            this.dtm = dtm;
            this.mes = mes;
            this.ano = ano;
            this.dbName = "dtbCTe" + ano;
            this.tableName = String.format("[dbo].[tbdCTeXMLMovimento%02d]", mes);
        }
    }

    private static final Map<String, DtmInfo> MASTER_DTM_LIST = new HashMap<>();

    static {
        System.out.println("Populando mapas de dados...");
        
        String dataChaves = """
        """; // Deixado em branco, conforme o original

        String[] linesChaves = dataChaves.split("\n");
        for (String line : linesChaves) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2) { // Precisa de pelo menos DTM e CHAVE
                String dtm = parts[0].trim();
                String chave = parts[parts.length - 1].trim().replace("\"", "");
                if (chave.length() == 44) {
                    DTM_PARA_CHAVE_MAP.put(dtm, chave);
                }
            }
        }
        System.out.println("Mapa de Chaves populado com " + DTM_PARA_CHAVE_MAP.size() + " DTMs que possuem chave.");

        
        // --- PARTE 2: Popular a MASTER_DTM_LIST (Lista Mestra com datas) ---
        
        // =======================================================================
        // ============ INÍCIO DA 1ª ALTERAÇÃO (LISTA DE DTMs) ===================
        // =======================================================================
        // AJUSTE: Lista de DTMs substituída pela lista fornecida pelo usuário.
        String dataDtmsMestra = """
        200091842	SEM CT-E 	03/02/2025
        200092720	SEM CT-E 	06/02/2025
        200093446	SEM CT-E 	11/02/2025
        200094051	SEM CT-E 	14/02/2025
        200094712	SEM CT-E 	18/02/2025
        200097432	SEM CT-E 	05/03/2025
        200097552	SEM CT-E 	06/03/2025
        200098376	SEM CT-E 	11/03/2025
        200098481	SEM CT-E 	12/03/2025
        200098705	SEM CT-E 	13/03/2025
        200099625	SEM CT-E 	19/03/2025
        200099892	SEM CT-E 	19/03/2025
        200102617	SEM CT-E 	02/04/2025
        200103302	SEM CT-E 	04/04/2025
        200106599	SEM CT-E 	23/04/2025
        200107089	SEM CT-E 	24/04/2025
        200107702	SEM CT-E 	28/04/2025
        200108199	SEM CT-E 	30/04/2025
        200108437	SEM CT-E 	02/05/2025
        200113973	SEM CT-E 	29/05/2025
        200114597	SEM CT-E 	02/06/2025
        200115062	SEM CT-E 	04/06/2025
        200116693	SEM CT-E 	12/06/2025
        200116938	SEM CT-E 	12/06/2025
        200117002	SEM CT-E 	13/06/2025
        200119213	SEM CT-E 	02/07/2025
        200119491	SEM CT-E 	27/06/2025
        200121551	SEM CT-E 	08/07/2025
        200122061	SEM CT-E 	10/07/2025
        200123341	SEM CT-E 	16/07/2025
        200123385	SEM CT-E 	16/07/2025
        200123413	SEM CT-E 	16/07/2025
        200123602	SEM CT-E 	17/07/2025
        200125390	SEM CT-E 	25/07/2025
        200125418	SEM CT-E 	25/07/2025
        200125821	SEM CT-E 	28/07/2025
        200126886	785970	05/08/2025
        200126958	786016	31/07/2025
        200127341	786570	04/08/2025
        200127556	787110	04/08/2025
        200127807	786612	05/08/2025
        200128081	786442	07/08/2025
        200128258	SEM CT-E 	06/08/2025
        200129094	SEM CT-E 	11/08/2025
        200129099	SEM CT-E 	11/08/2025
        200129109	SEM CT-E 	11/08/2025
        200129118	SEM CT-E 	11/08/2025
        200129637	SEM CT-E 	13/08/2025
        200130141	SEM CT-E 	15/08/2025
        200130788	SEM CT-E 	19/08/2025
        200131513	SEM CT-E 	21/08/2025
        200131629	SEM CT-E 	21/08/2025
        200136192	SEM CT-E 	15/09/2025
        200136868	SEM CT-E 	17/09/2025
        200137057	SEM CT-E 	18/09/2025
        200137088	SEM CT-E 	18/09/2025
        200137138	SEM CT-E 	18/09/2025
        200137909	SEM CT-E 	23/09/2025
        200138320	SEM CT-E 	25/09/2025
        200138356	SEM CT-E 	25/09/2025
        200138698	SEM CT-E 	29/09/2025
        200139201	SEM CT-E 	30/09/2025
        """;
        // =======================================================================
        // ============ FIM DA 1ª ALTERAÇÃO (LISTA DE DTMs) ======================
        // =======================================================================
        
        String[] linesDtms = dataDtmsMestra.split("\n");
        
        // =======================================================================
        // ============ INÍCIO DA 2ª ALTERAÇÃO (PARSER DE DATA) ==================
        // =======================================================================
        Pattern datePattern = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})$"); // Regex para dd/MM/yyyy (4 dígitos)
        
        for (String line : linesDtms) {
             String[] parts = line.trim().split("\\s+"); // Divide por um ou mais espaços
             if (parts.length >= 2) { // Precisa de pelo menos DTM e DATA
                 String dtm = parts[0].trim();
                 String dateStr = parts[parts.length - 1].trim();
                 
                 Matcher m = datePattern.matcher(dateStr);
                 
                 if (dtm.length() > 5 && m.find()) { // Checa se é DTM válida E tem data
                      try {
                          int mes = Integer.parseInt(m.group(2));
                          int ano = Integer.parseInt(m.group(3)); // Pega os 4 dígitos direto (ex: 2025)
                          
                          // Salva a informação de data para TODAS as DTMs
                          MASTER_DTM_LIST.put(dtm, new DtmInfo(dtm, mes, ano));
                          
                     } catch (NumberFormatException e) {
                         System.err.println("Erro ao parsear data: " + line);
                     }
                 }
             }
        }
        // =======================================================================
        // ============ FIM DA 2ª ALTERAÇÃO (PARSER DE DATA) =====================
        // =======================================================================
        
        System.out.println("Lista Mestra populada com " + MASTER_DTM_LIST.size() + " DTMs com data.");
    }


    //Classe para guardar resultado da busca do XML (Simplificada)
    private static class XmlInfo {
        String xmlContent;
        String idMovimento;
        String tabelaFonte;
        String termoBusca; // A CHAVE ou DTM que foi buscada
        String dtmParaSalvar; // A DTM associada (para nome da pasta)

        XmlInfo(String content, String idMov, String tabela, String termo) {
            this.xmlContent = content;
            this.idMovimento = idMov;
            this.tabelaFonte = tabela;
            this.termoBusca = termo;
        }
    }

    public static void main(String[] args) {
        System.out.println("--- Ferramenta de SELECT e Download de XML (Lógica 2 Passos) ---");

        // Lista de DTMs que AINDA NÃO FORAM ENCONTRADOS.
        Set<String> dtmsPendentes = new HashSet<>(MASTER_DTM_LIST.keySet());
        
        System.out.println(String.format("Iniciando busca por %d DTMs...", dtmsPendentes.size()));

        // Lista para guardar os resultados antes de salvar
        List<XmlInfo> resultadosEncontrados = new ArrayList<>();

        
        // ======================================================================
        // PASSO 1: BUSCA POR CHAVE CTe (BROADCAST)
        // ======================================================================
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PASSO 1: Iniciando busca por CHAVES CTe (Broadcast)...");
        System.out.println("Procurando chaves para DTMs que as possuem...");
        
        for (String dbName : DATABASES_TO_SEARCH) {
            if (dtmsPendentes.isEmpty()) break;

            System.out.println("\n" + "=".repeat(60));
            System.out.println("PASSO 1 [Banco: " + dbName + "]");
            
            String connectionUrl = String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;",
                    SQLSERVER_SERVER, SQLSERVER_PORT, dbName
            );

            try (Connection conn = DriverManager.getConnection(connectionUrl, SQLSERVER_USER, SQLSERVER_PASSWORD)) {
                
                for (int mes = 1; mes <= 12; mes++) {
                    if (dtmsPendentes.isEmpty()) break;
                    
                    String nomeTabela = String.format("[dbo].[tbdCTeXMLMovimento%02d]", mes);
                    System.out.println("\n" + "-".repeat(60));
                    System.out.println(String.format("PASSO 1 [Tabela: %s.%s]", dbName, nomeTabela));

                    // Itera sobre uma cópia dos pendentes
                    List<String> dtmsNestaTabela = new ArrayList<>(dtmsPendentes);
                    
                    for (String dtm : dtmsNestaTabela) {
                        // Verifica se esta DTM tem uma chave
                        String chaveCte = DTM_PARA_CHAVE_MAP.get(dtm); // <- Isso será sempre null (dataChaves está vazia)
                        
                        if (chaveCte != null) {
                            // SIM, tem uma chave. Procurar por ela.
                            try {
                                XmlInfo xmlInfo = buscarXmlEmTabelaUnica(conn, chaveCte, nomeTabela, dbName);
                                
                                if (xmlInfo != null) {
                                    System.out.println(String.format("   -> SUCESSO (Chave): DTM %s encontrada pela chave %s em %s", dtm, chaveCte.substring(0, 6)+"...", nomeTabela));
                                    xmlInfo.dtmParaSalvar = dtm; // Define a pasta de salvamento
                                    resultadosEncontrados.add(xmlInfo);
                                    dtmsPendentes.remove(dtm); // Remove da lista principal
                                }
                            } catch (SQLException e) {
                                String msgErro = e.getMessage().toLowerCase();
                                if (msgErro.contains("invalid object name") || msgErro.contains("objeto inválido")) {
                                    System.err.println("   -> AVISO: Tabela " + nomeTabela + " não existe. Pulando mês.");
                                    break; // Sai do loop de DTMs e vai pro próximo mês
                                } else {
                                    System.err.println("   -> ERRO SQL (Passo 1) ao buscar chave para DTM " + dtm + ": " + e.getMessage());
                                }
                            }
                        }
                        // Se não tem chave (chaveCte == null), ignora neste passo.
                    }
                } // fim loop mes
            } catch (SQLException e) {
                System.err.println("ERRO CRÍTICO de Conexão SQL (Passo 1) com " + dbName + ": " + e.getMessage());
            }
        } // fim loop banco

        
        // ======================================================================
        // PASSO 2: BUSCA POR NÚMERO DTM (OTIMIZADA)
        // ======================================================================
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PASSO 2: Iniciando busca por NÚMERO DTM (Otimizado por data)...");
        System.out.println(dtmsPendentes.size() + " DTMs restantes (SEM CT-E, falta, pdf, ou chave não encontrada).");
        
        if (dtmsPendentes.isEmpty()) {
             System.out.println("Nenhuma DTM pendente para o Passo 2.");
        }

        for (String dbName : DATABASES_TO_SEARCH) {
            if (dtmsPendentes.isEmpty()) break;

            System.out.println("\n" + "=".repeat(60));
            System.out.println("PASSO 2 [Banco: " + dbName + "]");
            
            String connectionUrl = String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;",
                    SQLSERVER_SERVER, SQLSERVER_PORT, dbName
            );

            try (Connection conn = DriverManager.getConnection(connectionUrl, SQLSERVER_USER, SQLSERVER_PASSWORD)) {
                
                for (int mes = 1; mes <= 12; mes++) {
                    if (dtmsPendentes.isEmpty()) break;
                    
                    String nomeTabela = String.format("[dbo].[tbdCTeXMLMovimento%02d]", mes);
                    System.out.println("\n" + "-".repeat(60));
                    System.out.println(String.format("PASSO 2 [Tabela: %s.%s]", dbName, nomeTabela));

                    // Itera sobre uma cópia dos pendentes
                    List<String> dtmsNestaTabela = new ArrayList<>(dtmsPendentes);
                    
                    for (String dtm : dtmsNestaTabela) {
                        // Pega a info de data desta DTM
                        DtmInfo info = MASTER_DTM_LIST.get(dtm);

                        // Proteção caso a DTM não esteja no mapa (embora não deva acontecer)
                        if (info == null) {
                            System.err.println("   -> ERRO: DTM " + dtm + " não encontrada no MASTER_DTM_LIST. Pulando.");
                            continue;
                        }
                        
                        // *** A MÁGICA DA OTIMIZAÇÃO ACONTECE AQUI ***
                        // Se a DTM não pertence a este banco (ano) OU a este mês, PULA.
                        // Ex: DTM 200091842 (03/02/2025) -> info.dbName = "dtbCTe2025", info.mes = 2
                        //     Se o loop estiver em dbName="dtbCTe2024", pula.
                        //     Se o loop estiver em dbName="dtbCTe2025" e mes=1, pula.
                        //     Se o loop estiver em dbName="dtbCTe2025" e mes=2, EXECUTA.
                        if (!info.dbName.equalsIgnoreCase(dbName) || info.mes != mes) {
                            continue; 
                        }
                        
                        // Se chegou aqui, é a tabela certa. Buscar pelo NÚMERO DA DTM
                        System.out.println(String.format("   -> Busca DIRECIONADA para DTM '%s' nesta tabela...", dtm));
                        try {
                            XmlInfo xmlInfo = buscarXmlEmTabelaUnica(conn, dtm, nomeTabela, dbName);
                            
                            if (xmlInfo != null) {
                                System.out.println(String.format("   -> SUCESSO (DTM): DTM %s encontrada pelo seu número em %s", dtm, nomeTabela));
                                xmlInfo.dtmParaSalvar = dtm; // Define a pasta de salvamento
                                resultadosEncontrados.add(xmlInfo);
                                dtmsPendentes.remove(dtm); // Remove da lista principal
                            }
                        } catch (SQLException e) {
                            String msgErro = e.getMessage().toLowerCase();
                            if (msgErro.contains("invalid object name") || msgErro.contains("objeto inválido")) {
                                System.err.println("   -> AVISO: Tabela " + nomeTabela + " não existe. Pulando mês.");
                                break; // Sai do loop de DTMs e vai pro próximo mês
                            } else {
                                System.err.println("   -> ERRO SQL (Passo 2) ao buscar DTM " + dtm + ": " + e.getMessage());
                            }
                        }
                    }
                } // fim loop mes
            } catch (SQLException e) {
                System.err.println("ERRO CRÍTICO de Conexão SQL (Passo 2) com " + dbName + ": " + e.getMessage());
            }
        } // fim loop banco
        

        // --- FASE DE DOWNLOAD ---
        System.out.println("\n" + "=".repeat(60));
        System.out.println("--- Fase de Download ---");
        if (resultadosEncontrados.isEmpty()) {
            System.out.println("Nenhum XML foi encontrado para download.");
        } else {
            System.out.println("Salvando " + resultadosEncontrados.size() + " XMLs encontrados...");
            for (XmlInfo xmlInfo : resultadosEncontrados) {
                salvarArquivo(xmlInfo); // Salva o XML
            }
        }

        // --- RESUMO FINAL ---
        System.out.println("\n" + "=".repeat(60));
        System.out.println("--- Processo Finalizado ---");
        if (!dtmsPendentes.isEmpty()) {
            System.err.println("\n--- RESUMO: DTMs NÃO encontradas (" + dtmsPendentes.size() + ") ---");
            for (String dtmPendente : dtmsPendentes) {
                DtmInfo info = MASTER_DTM_LIST.get(dtmPendente);
                if (info != null) {
                    System.err.println("- DTM: " + dtmPendente + " (Esperada em " + info.dbName + "/" + info.mes + ")");
                } else {
                    System.err.println("- DTM: " + dtmPendente + " (Não estava no mapa de datas)");
                }
            }
        } else {
            System.out.println("\n--- RESUMO: Todas as DTMs foram encontradas e processadas! ---");
        }
    }
    
    /**
     * Procura UM termo (chave ou DTM) em UMA tabela específica.
     */
    private static XmlInfo buscarXmlEmTabelaUnica(Connection conn, String termoBusca, String nomeTabela, String dbUsado) throws SQLException {
        // Limpa o termo de busca (remove aspas, se houver)
        String termoLimpo = termoBusca.replace("\"", "").trim();
        String termoLike = "%" + termoLimpo + "%";
        
        String sqlLike = String.format("SELECT TOP 1 ds_XML, id_Movimento FROM %s WITH (NOLOCK) WHERE ds_XML LIKE ?", nomeTabela);

        try (PreparedStatement pstmt = conn.prepareStatement(sqlLike)) {
            pstmt.setString(1, termoLike); 
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String idMovimentoEncontrado = rs.getString("id_Movimento");
                    // Retorna o objeto. A DTM será preenchida no loop 'main'
                    return new XmlInfo(rs.getString("ds_XML"), idMovimentoEncontrado, dbUsado + "." + nomeTabela, termoBusca);
                }
            }
        } catch (SQLException e) {
            throw e; // Relança a exceção
        }
        
        return null; // Não encontrou
    }


    /**
     * Salva o conteúdo XML em um arquivo. 
     * Usa a DTM (xmlInfo.dtmParaSalvar) no nome da pasta e no prefixo do arquivo.
     */
    private static void salvarArquivo(XmlInfo xmlInfo) { 
        String xmlContent = xmlInfo.xmlContent;
        if (xmlContent == null || xmlContent.isEmpty()) {
            System.err.println("   -> AVISO: Tentativa de salvar XML vazio para o termo: " + xmlInfo.termoBusca);
            return;
        }
        
        if (xmlInfo.dtmParaSalvar == null || xmlInfo.dtmParaSalvar.isEmpty()) {
             System.err.println("   -> ERRO FATAL: dtmParaSalvar está nula. Termo de busca: " + xmlInfo.termoBusca);
             return;
        }

        try {
            String chaveCteExtraida = extrairChaveDoXml(xmlContent);
            
            // Define o nome base (DTM) para a pasta e o arquivo
            String nomeBaseDTM = xmlInfo.dtmParaSalvar.replace("/", "-").trim();
            String nomeArquivo;

            // 1. Define o nome da PASTA usando a DTM
            String nomePasta = nomeBaseDTM; 

            // 2. Define o nome do ARQUIVO prefixando a DTM
            if (chaveCteExtraida != null) {
                // Formato: 200131782_352510...8870.xml
                nomeArquivo = nomeBaseDTM + "_" + chaveCteExtraida + ".xml";
            } else {
                // Se não achou chave no XML, usa o próprio termo de busca (que pode ser a chave ou a DTM)
                String nomeSufixo = xmlInfo.termoBusca.replace("\"", "").trim();
                
                // Se o termo de busca for a DTM, não precisa repetir
                if (nomeSufixo.equals(nomeBaseDTM)) {
                    nomeArquivo = nomeBaseDTM + "_semChaveNoXml.xml";
                } else {
                     nomeArquivo = nomeBaseDTM + "_" + nomeSufixo + "_semChaveNoXml.xml";
                }
                System.err.println("   -> AVISO: Chave CTe não extraída do XML. Usando Termo de Busca no nome (Termo: " + xmlInfo.termoBusca + ").");
            }

            String diretorioDestino = PASTA_RAIZ_DOWNLOAD + nomePasta + File.separator; 

            // *** CORREÇÃO APLICADA AQUI ***
            File diretorio = new File(diretorioDestino);
            if (!diretorio.exists()) {
                if (!diretorio.mkdirs()) {
                    System.err.println("   -> ERRO AO CRIAR PASTA: Não foi possível criar " + diretorioDestino);
                    return;
                }
            }

            String caminhoCompleto = diretorioDestino + nomeArquivo;
            
            // Verifica se o arquivo já existe
            File f = new File(caminhoCompleto);
            if (f.exists()) {
                System.out.println("   -> AVISO: Arquivo já existe, pulando o salvamento. Caminho: " + caminhoCompleto);
                return;
            }

            try (FileWriter writer = new FileWriter(caminhoCompleto)) {
                writer.write(xmlContent);
                System.out.println("   -> XML salvo em: " + caminhoCompleto);
            } catch (IOException e) {
                System.err.println("   -> ERRO AO SALVAR ARQUIVO: " + e.getMessage() + " | Caminho: " + caminhoCompleto);
            }
        } catch (Exception e) {
            System.err.println("   -> ERRO GERAL AO SALVAR ARQUIVO para termo " + xmlInfo.termoBusca + ": " + e.getMessage());
        }
    }

    /**
     * Extrai a chave do CTe de 44 dígitos de dentro do XML.
     * (Método inalterado)
     */
    private static String extrairChaveDoXml(String xmlContent) {
        if (xmlContent == null || xmlContent.isEmpty()) return null;
        try {
            // Busca por Id="CTe<44 digitos>"
            Pattern patternId = Pattern.compile("Id=\"CTe(\\d{44})\"");
            Matcher matcherId = patternId.matcher(xmlContent);
            if (matcherId.find()) {
                return matcherId.group(1);
            }
            // Busca por <chCTe>44 digitos</chCTe>
            Pattern patternCh = Pattern.compile("<chCTe>(\\d{44})</chCTe>");
            Matcher matcherCh = patternCh.matcher(xmlContent);
            if (matcherCh.find()) {
                return matcherCh.group(1);
            }
        } catch (Exception e) {
            System.err.println("   -> ERRO (interno): Falha ao extrair chave CTe do XML. Detalhes: " + e.getMessage());
        }
        return null;
    }
}