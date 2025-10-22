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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ferramenta simplificada para buscar XMLs de CTe em lote.
 * Percorre as tabelas tbdCTeXMLMovimento01 a 10 (Banco 2025) e busca
 * termos (Minuta/CT-e) no campo ds_XML.
 */
public class BaixarXmlCte { 

    // --- CONFIGURAÇÕES DO BANCO SQL SERVER (CTe) ---
    private static final String SQLSERVER_SERVER = "192.168.10.251";
    private static final String SQLSERVER_PORT = "1433";
    private static final String SQLSERVER_USER = "sa";
    private static final String SQLSERVER_PASSWORD = "esl@13509";
    
    private static final String SQLSERVER_DB_CTE_2025 = "dtbCTe2024"; 
    private static final String PASTA_RAIZ_DOWNLOAD = "C:\\XMLs_Busca_Simples\\"; 
    
    // Lista de termos (Minuta/CT-e) a serem buscados via LIKE no ds_XML
    private static final List<String> TERMOS_DE_PESQUISA = Arrays.asList(
        "27270-3", 
        "75512-4", 
        "75521-4", 
        "27274-3", 
        "75474-4", 
        "75531-4", 
        "75511-4", 
        "27271-3", 
        "27275-3", 
        "27272-3", 
        "75520-4", 
        "27266-3", 
        "27273-3", 
        "27264-3", 
        "27276-3", 
        "27268-3"
        // Adicione outros termos aqui, se necessário.
    );

    // Classe para guardar resultado da busca do XML (Simplificada)
    private static class XmlInfo {
        String xmlContent;
        String idMovimento;
        String tabelaFonte;
        String termoBusca;

        XmlInfo(String content, String idMov, String tabela, String termo) {
            this.xmlContent = content;
            this.idMovimento = idMov;
            this.tabelaFonte = tabela;
            this.termoBusca = termo;
        }
    }


    public static void main(String[] args) {
        System.out.println("--- Ferramenta de SELECT e Download de XML (Busca SIMPLES por LIKE no ds_XML) ---");
        System.out.println("Banco de CT-e: " + SQLSERVER_DB_CTE_2025);
        System.out.println(String.format("Buscando %d termos nas tabelas 01 a 10...", TERMOS_DE_PESQUISA.size()));
        
        // URL de Conexão
        String urlCte2025 = String.format(
            "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;",
            SQLSERVER_SERVER, SQLSERVER_PORT, SQLSERVER_DB_CTE_2025
        );

        List<String> itensNaoEncontrados = new ArrayList<>(TERMOS_DE_PESQUISA); // Começa com todos como não encontrados

        // Abre conexão
        try (Connection connCte2025 = DriverManager.getConnection(urlCte2025, SQLSERVER_USER, SQLSERVER_PASSWORD)) {

            for (String termoBusca : TERMOS_DE_PESQUISA) {
                System.out.println("\n" + "-".repeat(60));
                System.out.println(String.format("Processando Termo: %s", termoBusca));

                try {
                    // Busca sequencial nas tabelas 01 a 10
                    XmlInfo xmlInfo = buscarXmlEmTabelas(connCte2025, termoBusca);

                    if (xmlInfo != null && xmlInfo.xmlContent != null && !xmlInfo.xmlContent.isEmpty()) {
                        salvarArquivo(xmlInfo); // Salva o XML se encontrado
                        itensNaoEncontrados.remove(termoBusca); // Remove da lista de não encontrados
                    } else {
                        System.err.println("  -> FALHA FINAL: XML não encontrado em nenhuma tabela (ou estava vazio).");
                    }
                } catch (Exception e) {
                    System.err.println("  -> ERRO CRÍTICO no processamento do Termo " + termoBusca + ": " + e.getMessage());
                }
            } // fim do loop for

        } catch (SQLException e) {
            System.err.println("ERRO CRÍTICO de Conexão SQL (Verifique as configs do Banco): " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("--- Processo Finalizado ---");
        if (!itensNaoEncontrados.isEmpty()) {
            System.err.println("\n--- RESUMO: Itens NÃO encontrados ou com Erro ---");
            for (String itemInfo : itensNaoEncontrados) {
                System.err.println("- Termo: " + itemInfo);
            }
        } else {
            System.out.println("\n--- RESUMO: Todos os XMLs foram encontrados e processados! ---");
        }
    }
    
    /**
     * Procura o termo nas tabelas tbdCTeXMLMovimento01 até 10 do banco 2025.
     */
    private static XmlInfo buscarXmlEmTabelas(Connection conn, String termoBusca) throws SQLException {
        String dbUsado = SQLSERVER_DB_CTE_2025;
        String termoLike = "%" + termoBusca.replace("/", "").trim() + "%";
        
        // Itera pelas tabelas 01 a 10
        for (int mes = 1; mes <= 12; mes++) {
            String nomeTabela = String.format("[dbo].[tbdCTeXMLMovimento%02d]", mes);
            System.out.println(String.format("  -> Verificando em %s.%s com LIKE '%s'", dbUsado, nomeTabela, termoLike));
            
            // SQL de busca
            String sqlLike = String.format("SELECT TOP 1 ds_XML, id_Movimento FROM %s WITH (NOLOCK) WHERE ds_XML LIKE ?", nomeTabela);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlLike)) {
                pstmt.setString(1, termoLike); 
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String idMovimentoEncontrado = rs.getString("id_Movimento");
                        System.out.println("     -> SUCESSO! XML encontrado. ID_Movimento: " + idMovimentoEncontrado);
                        return new XmlInfo(rs.getString("ds_XML"), idMovimentoEncontrado, dbUsado + "." + nomeTabela, termoBusca);
                    }
                }
            } catch (SQLException e) {
                 // Captura a exceção (e.g. tabela não existe) e continua para a próxima
                 // System.err.println("     -> AVISO: Tabela " + nomeTabela + " não acessível. Pulando.");
            }
        }
        
        return null;
    }


    /**
     * Salva o conteúdo XML em um arquivo. Usa o termo de busca (Minuta/CT-e) no nome do arquivo 
     * e o id_Movimento como nome da pasta, se disponível.
     */
    private static void salvarArquivo(XmlInfo xmlInfo) { 
        String xmlContent = xmlInfo.xmlContent;
        if (xmlContent == null || xmlContent.isEmpty()) {
            return;
        }
        try {
            String chaveCte = extrairChaveDoXml(xmlContent);
            String nomeArquivo;
            // Usa o termo de busca no nome do arquivo e o idMovimento na pasta
            String nomePasta = xmlInfo.idMovimento != null ? xmlInfo.idMovimento : xmlInfo.termoBusca; 

            if (chaveCte != null) {
                nomeArquivo = chaveCte + ".xml";
            } else {
                // Nome do arquivo baseado no termo de busca se a chave não for encontrada
                nomeArquivo = xmlInfo.termoBusca.replace("/", "-").trim() + "_semChave.xml";
                System.err.println("  -> AVISO: Chave CTe não extraída do XML. Usando termo de busca no nome do arquivo.");
            }

            String diretorioDestino = PASTA_RAIZ_DOWNLOAD + nomePasta + File.separator; 

            File diretorio = new File(diretorioDestino);
            if (!diretorio.exists()) {
                if (!diretorio.mkdirs()) {
                    System.err.println("  -> ERRO AO CRIAR PASTA: Não foi possível criar " + diretorioDestino);
                    return;
                }
            }

            String caminhoCompleto = diretorioDestino + nomeArquivo;
            try (FileWriter writer = new FileWriter(caminhoCompleto)) {
                writer.write(xmlContent);
                System.out.println("  -> XML salvo em: " + caminhoCompleto);
            } catch (IOException e) {
                System.err.println("  -> ERRO AO SALVAR ARQUIVO: " + e.getMessage() + " | Caminho: " + caminhoCompleto);
            }
        } catch (Exception e) {
            System.err.println("  -> ERRO GERAL AO SALVAR ARQUIVO para termo " + xmlInfo.termoBusca + ": " + e.getMessage());
        }
    }


    /**
     * Extrai a chave do CTe de 44 dígitos de dentro do XML. (Inalterado)
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
            System.err.println("  -> ERRO (interno): Falha ao extrair chave CTe do XML. Detalhes: " + e.getMessage());
        }
        return null;
    }
}