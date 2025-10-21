package com.tecnolog.autocoleta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ferramenta autônoma para BUSCAR XMLs de CTe em lote (Lógica V13 - PG + SQLSrv).
 * FLUXO:
 * 1. Pega DTM da lista.
 * 2. Busca id_PedidoColeta no POSTGRES (tbddtmocorrencias).
 * 3. Se achar, busca Movimento no SQL Server por id_PedidoColeta.
 * 4. Se NÃO achar no PG, busca Movimento no SQL Server por nr_Referencia (DTM).
 * 5. Com Movimento, busca XML no dtbCTe e salva.
 */
public class BaixarXmlCte { // Nome da classe

    // =========================================================================
    // --- PREENCHA AQUI AS CONFIGURAÇÕES DO BANCO POSTGRESQL (DTMs) ---
    // =========================================================================
    private static final String POSTGRES_SERVER = "localhost"; // Ex: 192.168.10.100
    private static final String POSTGRES_PORT = "5432";
    private static final String POSTGRES_DB = "dtbSyncLog_Hmg"; // O nome do seu banco Postgres
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "BRU*980192@no";
    // =========================================================================

    // --- CONFIGURAÇÕES DOS BANCOS SQL SERVER (PRODUÇÃO) ---
    private static final String SQLSERVER_SERVER = "192.168.10.251";
    private static final String SQLSERVER_PORT = "1433";
    private static final String SQLSERVER_USER = "sa";
    private static final String SQLSERVER_PASSWORD = "esl@13509";
    private static final String SQLSERVER_DB_TRANSPORTE = "dtbTransporteHomologacao"; // Banco de Movimentos (AJUSTADO PARA HMG)
    private static final String SQLSERVER_DB_CTE = "dtbCTe2025";             // Banco onde ficam as tabelas tbdCTeXMLMovimentoMM
    private static final String PASTA_RAIZ_DOWNLOAD = "C:\\DTM_XMLs_Producao\\"; // Pasta para salvar os XMLs

    // Classe para guardar informações do movimento
    private static class MovimentoInfo {
        String idMovimento;
        java.sql.Date dataMovimento;
        String metodoBusca; // Como foi encontrado (PG ou SQLSrv)
        MovimentoInfo(String id, java.sql.Date data, String metodo) { 
            this.idMovimento = id; 
            this.dataMovimento = data;
            this.metodoBusca = metodo;
        }
    }

     // Classe para guardar resultado da busca do XML
     private static class XmlInfo {
         String xmlContent;
         String metodoBusca;
         String tabelaFonte; // Qual tbdCTeXMLMovimentoMM foi usada

         XmlInfo(String content, String metodo, String tabela) {
             this.xmlContent = content;
             this.metodoBusca = metodo;
             this.tabelaFonte = tabela;
         }
    }


    public static void main(String[] args) {
        System.out.println("--- Ferramenta de Busca de XML (Lógica V13 - PG/SQLSrv) ---");
        System.out.println("Usando Banco de Transporte: " + SQLSERVER_DB_TRANSPORTE);
        System.out.println("Usando Banco de CT-e: " + SQLSERVER_DB_CTE);

        // *** ALTERAÇÃO SOLICITADA: Lista de 311 DTMs ***
        List<String> dtmsParaBuscar = Arrays.asList(
            "200058546", "200078933", "200079639", "200080032", "200080455",
            "200080505", "200080713", "200080727", "200080758", "200080807",
            "200080976", "200081056", "200081062", "200081101", "200081103",
            "200081240", "200081482", "200081672", "200081741", "200081845",
            "200082420", "200083397", "200083436", "200083959", "200084033",
            "200084239", "200084734", "200085501", "200085522", "200085637",
            "200085755", "200085806", "200085808", "200085827", "200086406",
            "200086893", "200086902", "200087185", "200087247", "200087254",
            "200088479", "200089163", "200089231", "200089832", "200090043",
            "200090545", "200091842", "200092720", "200093446", "200094051",
            "200094207", "200094712", "200097432", "200097552", "200098367",
            "200098376", "200098481", "200098550", "200098705", "200099046",
            "200099625", "200099892", "200102617", "200103302", "200103808",
            "200105138", "200106599", "200107089", "200107702", "200108199",
            "200108437", "200110026", "200111035", "200112384", "200113907",
            "200113973", "200114597", "200115062", "200116693", "200116938",
            "200117002", "200119213", "200119491", "200121551", "200122061",
            "200123341", "200123385", "200123413", "200123602", "200125190",
            "200125390", "200125418", "200125821", "200126886", "200126958",
            "200127341", "200127556", "200127807", "200128081", "200128258",
            "200129094", "200129099", "200129109", "200129118", "200129637",
            "200130141", "200130788", "200131513", "200131629", "200131782",
            "200131879", "200132952", "200135134", "200136192", "200136868",
            "200137057", "200137088", "200137138", "200137909", "200137954",
            "200138015", "200138268", "200138285", "200138320", "200138356",
            "200138357", "200138461", "200138489", "200138554", "200138698",
            "200139018", "200139025", "200139088", "200139201", "200139497",
            "200139508", "200139607", "200139614", "200139626", "200139847",
            "200140024", "200140140", "200140168", "200140197", "200140199",
            "200140201", "200140223", "200140225", "200140264", "200140282",
            "200140408", "200140416", "200140509", "200140624", "200140628",
            "200140661", "200140749", "200140760", "200140763", "200140817",
            "200140824", "200140829", "200140869", "200141009", "200141045",
            "200141046", "200141060", "200141110", "200141117", "200141119",
            "200141120", "200141143", "200141144", "200141210", "200141223",
            "200141272", "200141292", "200141317", "200141350", "200141353",
            "200141359", "200141360", "200141376", "200141380", "200141382",
            "200141405", "200141416", "200141425", "200141429", "200141431",
            "200141432", "200141433", "200141435", "200141441", "200141450",
            "200141456", "200141458", "200141464", "200141467", "200141471",
            "200141473", "200141474", "200141483", "200141488", "200141493",
            "200141511", "200141513", "200141522", "200141528", "200141547",
            "200141560", "200141602", "200141604", "200141607", "200141615",
            "200141619", "200141623", "200141680", "200141718", "200141761",
            "200141762", "200141764", "200141769", "200141786", "200141794",
            "200141796", "200141798", "200141801", "200141803", "200141807",
            "200141811", "200141816", "200141819", "200141824", "200141836",
            "200141837", "200141839", "200141847", "200141864", "200141868",
            "200141893", "200141903", "200141936", "200141940", "200141941",
            "200141954", "200141958", "200141964", "200141966", "200141967",
            "200141969", "200141974", "200141977", "200141979", "200141981",
            "200141987", "200142002", "200142008", "200142010", "200142019",
            "200142024", "200142033", "200142066", "200142067", "200142071",
            "200142075", "200142079", "200142083", "200142084", "200142085",
            "200142089", "200142093", "200142112", "200142126", "200142132",
            "200142137", "200142138", "200142143", "200142150", "200142153",
            "200142159", "200142167", "200142171", "200142181", "200142190",
            "200142192", "200142200", "200142204", "200142209", "200142210",
            "200142212", "200142230", "200142231", "200142232", "200142238",
            "200142247", "200142253", "200142261", "200142262", "200142264",
            "200142266", "200142269", "200142278", "200142281", "200142285",
            "2O0142293", // CUIDADO: Este item "2O0142293" começa com a letra 'O'
            "200142300", "200142301", "200142302", "200142303",
            "200078188"
        );

        List<String> itensNaoEncontrados = new ArrayList<>();

        // URLs de Conexão
        String urlPostgres = String.format("jdbc:postgresql://%s:%s/%s",
             POSTGRES_SERVER, POSTGRES_PORT, POSTGRES_DB
        );
        String urlTransporte = String.format(
             "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;",
             SQLSERVER_SERVER, SQLSERVER_PORT, SQLSERVER_DB_TRANSPORTE
        );
        String urlCte = String.format(
             "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;",
             SQLSERVER_SERVER, SQLSERVER_PORT, SQLSERVER_DB_CTE
        );


        // Abre conexão com todos os bancos
        try (Connection connPostgres = DriverManager.getConnection(urlPostgres, POSTGRES_USER, POSTGRES_PASSWORD);
             Connection connTransporte = DriverManager.getConnection(urlTransporte, SQLSERVER_USER, SQLSERVER_PASSWORD);
             Connection connCte = DriverManager.getConnection(urlCte, SQLSERVER_USER, SQLSERVER_PASSWORD)) {

            System.out.println("Conectado ao PG, SQLSrv-Transporte e SQLSrv-CTe. Iniciando busca para " + dtmsParaBuscar.size() + " DTMs...");

            for (String dtm : dtmsParaBuscar) {
                System.out.println("\n" + "-".repeat(60));
                System.out.println("Processando DTM: " + dtm);

                // 1. Buscar id_PedidoColeta no Postgres (Lógica V7)
                String idPedidoColeta = buscarIdPedidoColetaNoPostgres(connPostgres, dtm);
                MovimentoInfo movInfo = null;

                // 2. Buscar Movimento no SQL Server
                if (idPedidoColeta != null) {
                    System.out.println("  1. [PG] Encontrado id_PedidoColeta '" + idPedidoColeta + "' via tbddtmocorrencias.");
                    // 2a. Busca Movimento por id_PedidoColeta
                    movInfo = buscarMovimentoPorPedidoColeta(connTransporte, idPedidoColeta);
                } else {
                    System.err.println("  1. [PG] id_PedidoColeta NÃO encontrado em tbddtmocorrencias.");
                    System.out.println("  2. [SQL] Tentando fallback: buscando Movimento por nr_Referencia (DTM) no SQL Server...");
                    // 2b. Fallback: Busca Movimento por nr_Referencia (a própria DTM)
                    movInfo = buscarMovimentoPorNrReferencia(connTransporte, dtm);
                }

                XmlInfo xmlInfo = null;

                // 3. Tentar buscar XML pelo id_Movimento
                if (movInfo != null && movInfo.idMovimento != null) {
                    System.out.println("  3. [SQL] Movimento encontrado (via " + movInfo.metodoBusca + "): id=" + movInfo.idMovimento + ", data=" + movInfo.dataMovimento);
                    xmlInfo = buscarXmlPorMovimento(connCte, movInfo.idMovimento, movInfo.dataMovimento);

                    if (xmlInfo != null) {
                         System.out.println("  4. [SQL-CTE] XML encontrado por id_Movimento na tabela " + xmlInfo.tabelaFonte);
                    } else {
                         System.err.println("  -> AVISO: Movimento encontrado, mas XML não localizado por id_Movimento.");
                    }
                } else {
                     System.err.println("  3. [SQL] Movimento NÃO encontrado para DTM " + dtm);
                }

                // 4. Se não achou pelo id_Movimento, tentar fallback buscando DTM dentro do XML
                if (xmlInfo == null) {
                    System.out.println("  4. [SQL-CTE] Tentando fallback: buscando '" + dtm + "' dentro do ds_XML...");
                    xmlInfo = buscarXmlPorConteudo(connCte, dtm);
                    if (xmlInfo != null) {
                        System.out.println("  5. [SQL-CTE] XML encontrado por fallback (busca no conteúdo) na tabela " + xmlInfo.tabelaFonte);
                    } else {
                         System.err.println("  -> FALHA FINAL: XML não encontrado por nenhum método.");
                         itensNaoEncontrados.add("DTM: " + dtm);
                    }
                }

                // 5. Salvar XML se encontrado
                if (xmlInfo != null && xmlInfo.xmlContent != null && !xmlInfo.xmlContent.isEmpty()) {
                    // Passa a DTM original para ser o nome da pasta
                    salvarArquivo(dtm, xmlInfo.xmlContent); 
                } else if (xmlInfo != null) {
                    System.err.println("  -> ERRO: XML encontrado (" + xmlInfo.metodoBusca + "), mas conteúdo está VAZIO.");
                    itensNaoEncontrados.add("DTM: " + dtm + " - XML Vazio");
                }

            } // fim do loop for

        } catch (SQLException e) {
            System.err.println("ERRO CRÍTICO de Conexão SQL: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("--- Processo Finalizado ---");
         if (!itensNaoEncontrados.isEmpty()) {
            System.err.println("\n--- RESUMO: Itens NÃO encontrados ou com XML Vazio ---");
            for (String itemInfo : itensNaoEncontrados) {
                System.err.println("- " + itemInfo);
            }
        } else {
            System.out.println("\n--- RESUMO: Todos os XMLs foram encontrados e processados! ---");
        }
    }


    /**
     * NOVO (V13): Tenta encontrar o id_PedidoColeta no banco PostgreSQL.
     * (Lógica do script V7)
     */
    private static String buscarIdPedidoColetaNoPostgres(Connection connPostgres, String dtm) {
        // Converte o ID_DTM para número, se a coluna for numérica no Postgres
        long idDtmLong;
        try {
            // Corrige o typo da DTM "2O0142293" (letra O) para "200142293" (numero 0)
            if (dtm.startsWith("2O")) {
                dtm = dtm.replaceFirst("O", "0");
                System.out.println("  -> Corrigindo typo DTM: " + dtm);
            }
            idDtmLong = Long.parseLong(dtm);
        } catch (NumberFormatException e) {
             System.err.println("  -> ERRO (Postgres): DTM '" + dtm + "' não é um número válido para tbddtmocorrencias.");
             return null;
        }

        String sqlPostgres = "SELECT ds_observacoes FROM public.tbddtmocorrencias " +
                             "WHERE id_dtm = ? AND ds_observacoes LIKE 'Coleta Nº - %' " +
                             "ORDER BY dt_inclusao DESC LIMIT 1";

        try (PreparedStatement pstmt = connPostgres.prepareStatement(sqlPostgres)) {
            
            pstmt.setLong(1, idDtmLong);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String observacao = rs.getString("ds_observacoes");
                    // Extrai o número de "Coleta Nº - 7698"
                    return extrairIdPedidoColeta(observacao);
                } else {
                    return null; // Não encontrado
                }
            }
        } catch (SQLException e) {
            System.err.println("  -> ERRO (Postgres): Falha ao consultar o PostgreSQL: " + e.getMessage());
            return null;
        }
    }

    /**
     * NOVO (V13): Helper para extrair o ID da string "Coleta Nº - 7698"
     * (Lógica do script V7)
     */
    private static String extrairIdPedidoColeta(String observacao) {
         if (observacao == null || !observacao.contains("Coleta Nº - ")) {
             return null;
         }
         try {
             // Pega tudo depois de "Coleta Nº - "
             String id = observacao.substring(observacao.indexOf("Coleta Nº - ") + "Coleta Nº - ".length());
             // Remove aspas e espaços extras
             id = id.replace("\"", "").trim();
             // Retorna apenas os números
             Matcher m = Pattern.compile("(\\d+)").matcher(id);
             if (m.find()) {
                 return m.group(1);
             }
             return null;
         } catch (Exception e) {
             System.err.println("  -> ERRO (Parser): Falha ao extrair ID da observação: " + observacao);
             return null;
         }
    }


    /**
     * Busca o MovimentoInfo (id e data) no SQL Server (dtbTransporte) usando id_PedidoColeta.
     */
    private static MovimentoInfo buscarMovimentoPorPedidoColeta(Connection connTransporte, String idPedidoColeta) {
        String sql = "SELECT TOP 1 id_Movimento, dt_Movimento FROM tbdMovimento WITH (NOLOCK) WHERE id_PedidoColeta = ?";

        try (PreparedStatement pstmt = connTransporte.prepareStatement(sql)) {
            pstmt.setString(1, idPedidoColeta);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new MovimentoInfo(rs.getString("id_Movimento"), rs.getDate("dt_Movimento"), "id_PedidoColeta(PG)");
                }
            }
        } catch (SQLException e) {
            System.err.println("  -> ERRO [SQL] ao buscar movimento por id_PedidoColeta " + idPedidoColeta + ": " + e.getMessage());
        }
        return null; // Não encontrado ou erro
    }

    /**
     * Busca o MovimentoInfo (id e data) no SQL Server (dtbTransporte) usando nr_Referencia (DTM).
     */
    private static MovimentoInfo buscarMovimentoPorNrReferencia(Connection connTransporte, String dtm) {
        String sql = "SELECT TOP 1 mov.id_Movimento, mov.dt_Movimento " +
                     "FROM tbdItemPedidoColeta item WITH (NOLOCK) " +
                     "JOIN tbdMovimento mov WITH (NOLOCK) ON item.id_PedidoColeta = mov.id_PedidoColeta " +
                     "WHERE item.nr_Referencia = ?";

        try (PreparedStatement pstmt = connTransporte.prepareStatement(sql)) {
            pstmt.setString(1, dtm);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new MovimentoInfo(rs.getString("id_Movimento"), rs.getDate("dt_Movimento"), "nr_Referencia(SQL)");
                }
            }
        } catch (SQLException e) {
            System.err.println("  -> ERRO [SQL] ao buscar movimento por nr_Referencia " + dtm + ": " + e.getMessage());
        }
        return null; // Não encontrado ou erro
    }


     /**
     * Busca o XML na tabela correta tbdCTeXMLMovimentoMM usando id_Movimento.
     */
    private static XmlInfo buscarXmlPorMovimento(Connection connCte, String idMovimento, java.sql.Date dataMovimento) {
        if (idMovimento == null) return null;

        LocalDate localDate;
        if (dataMovimento != null) {
            localDate = dataMovimento.toLocalDate();
        } else {
             System.err.println("  -> AVISO: dt_Movimento é NULA para id_Movimento " + idMovimento + ". Não é possível determinar tabela exata do XML.");
            return null;
        }

        String ano = String.valueOf(localDate.getYear());
        if (!SQLSERVER_DB_CTE.endsWith(ano)) {
             System.err.println("  -> AVISO: Ano do dt_Movimento ("+ano+") não corresponde ao banco CTE ("+SQLSERVER_DB_CTE+"). Verifique a config SQLSERVER_DB_CTE.");
             // Pode ser necessário ajustar o nome do banco dinamicamente aqui se os anos variarem
        }

        String mes = String.format("%02d", localDate.getMonthValue());
        String tableName = String.format("[dbo].[tbdCTeXMLMovimento%s]", mes);
        String sqlXml = String.format("SELECT ds_XML FROM %s WITH (NOLOCK) WHERE id_Movimento = ?", tableName);

        System.out.println("  -> Buscando XML por id_Movimento em " + SQLSERVER_DB_CTE + "." + tableName);

        try (PreparedStatement pstmt = connCte.prepareStatement(sqlXml)) {
            pstmt.setString(1, idMovimento);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new XmlInfo(rs.getString("ds_XML"), "id_Movimento", tableName);
                }
            }
        } catch (SQLException e) {
            // Se a tabela não existir (ex: mês 02), ele vai falhar aqui.
            System.err.println("  -> ERRO ao buscar XML por id_Movimento em " + tableName + ": " + e.getMessage());
        }
        return null; // Não encontrado ou erro
    }


     /**
     * Busca o XML procurando pelo ID/DTM DENTRO do ds_XML nas tabelas de fallback.
     */
    private static XmlInfo buscarXmlPorConteudo(Connection connCte, String idOuDtm) {
        List<String> tabelasFallback = Arrays.asList(
             "[dbo].[tbdCTeXMLMovimento10]", // Outubro
             "[dbo].[tbdCTeXMLMovimento11]", // Novembro
             "[dbo].[tbdCTeXMLMovimento12]", // Dezembro
             "[dbo].[tbdCTeXMLMovimento01]"  // Janeiro
             // Adicione mais tabelas aqui se necessário
        );

        for (String tableName : tabelasFallback) {
             System.out.println("  -> Buscando por conteúdo '" + idOuDtm + "' em " + SQLSERVER_DB_CTE + "." + tableName);
             String sqlXml = String.format("SELECT TOP 1 ds_XML, id_Movimento FROM %s WITH (NOLOCK) WHERE ds_XML LIKE ?", tableName);
             try (PreparedStatement pstmt = connCte.prepareStatement(sqlXml)) {
                 pstmt.setString(1, "%" + idOuDtm + "%");
                 try (ResultSet rs = pstmt.executeQuery()) {
                     if (rs.next()) {
                         System.out.println("     -> Encontrado (fallback) no id_Movimento: " + rs.getString("id_Movimento"));
                         return new XmlInfo(rs.getString("ds_XML"), "Conteúdo XML", tableName);
                     }
                 }
             } catch (SQLException e) {
                 // Ignora se a tabela não existir
                 if (e.getMessage().contains("Invalid object name")) {
                    System.out.println("     -> Tabela de fallback " + tableName + " não existe. Pulando.");
                 } else {
                    System.err.println("  -> ERRO ao buscar XML por conteúdo em " + tableName + ": " + e.getMessage());
                 }
             }
        }
        return null; // Não encontrado
    }

     /**
     * Salva o conteúdo XML em um arquivo, usando a DTM original como nome da pasta.
     */
    private static void salvarArquivo(String dtmOriginal, String xmlContent) { // Assinatura simplificada
        if (xmlContent == null || xmlContent.isEmpty()) {
            return;
        }
         try {
             String chaveCte = extrairChaveDoXml(xmlContent);
             String nomeArquivo;
             if (chaveCte != null) {
                 nomeArquivo = chaveCte + ".xml";
             } else {
                 // Usa a DTM original se não achar chave
                 nomeArquivo = dtmOriginal + "_semChave.xml";
                 System.err.println("  -> AVISO: Chave CTe não extraída do XML para DTM " + dtmOriginal + ". Usando DTM no nome do arquivo.");
             }

             // *** Usa dtmOriginal para o nome da pasta ***
             String diretorioDestino = PASTA_RAIZ_DOWNLOAD + dtmOriginal + File.separator;

             File diretorio = new File(diretorioDestino);
             if (!diretorio.exists()) {
                 System.out.println("  -> Criando pasta: " + diretorioDestino);
                 if (!diretorio.mkdirs()) {
                     System.err.println("  -> ERRO AO CRIAR PASTA: Não foi possível criar " + diretorioDestino);
                     return;
                 }
             }

             String caminhoCompleto = diretorioDestino + nomeArquivo;
             try (FileWriter writer = new FileWriter(caminhoCompleto)) {
                 writer.write(xmlContent);
                 System.out.println("  -> SUCESSO! XML salvo em: " + caminhoCompleto);
             } catch (IOException e) {
                 System.err.println("  -> ERRO AO SALVAR ARQUIVO: " + e.getMessage() + " | Caminho: " + caminhoCompleto);
             }
         } catch (Exception e) {
             System.err.println("  -> ERRO GERAL AO SALVAR ARQUIVO para DTM " + dtmOriginal + ": " + e.getMessage());
         }
    }


     /**
     * Extrai a chave do CTe de 44 dígitos de dentro do XML.
     */
    private static String extrairChaveDoXml(String xmlContent) {
         if (xmlContent == null || xmlContent.isEmpty()) return null;
         try {
             String tagInicioId = "Id=\"CTe";
             int startIndexId = xmlContent.indexOf(tagInicioId);
             if (startIndexId != -1) {
                 startIndexId += tagInicioId.length();
                 int endIndexId = xmlContent.indexOf("\"", startIndexId);
                 if (endIndexId != -1) {
                     String chave = xmlContent.substring(startIndexId, endIndexId);
                     if (chave.length() == 44 && chave.matches("\\d+")) return chave;
                 }
             }
             String tagInicioCh = "<chCTe>";
             int startIndexCh = xmlContent.indexOf(tagInicioCh);
             if (startIndexCh != -1) {
                 startIndexCh += tagInicioCh.length();
                 int endIndexCh = xmlContent.indexOf("</chCTe>", startIndexCh);
                 if (endIndexCh != -1) {
                     String chave = xmlContent.substring(startIndexCh, endIndexCh);
                      if (chave.length() == 44 && chave.matches("\\d+")) return chave;
                 }
             }
         } catch (Exception e) {
              System.err.println("  -> ERRO (interno): Falha ao extrair chave CTe do XML. Detalhes: " + e.getMessage());
         }
        return null;
    }

    // (O método criarMapaPedidoDtm() não é mais necessário e foi removido)
}