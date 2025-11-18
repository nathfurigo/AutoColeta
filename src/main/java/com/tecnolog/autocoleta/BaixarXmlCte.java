package com.tecnolog.autocoleta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaixarXmlCte {

    private static final String SQLSERVER_SERVER = "192.168.10.251";
    private static final String SQLSERVER_PORT = "1433";
    private static final String SQLSERVER_USER = "sa";
    private static final String SQLSERVER_PASSWORD = "esl@13509";
    private static final String PASTA_RAIZ_DOWNLOAD = "C:\\XMLs_Busca_Simples\\";
    private static final String DATABASE_BUSCA = "dtbCTe2024";
    private static final String TABELA_BUSCA = "[dbo].[tbdCTeXMLMovimento11]";

    private static class XmlInfo {
        String xmlContent;
        String idMovimento;
        String tabelaFonte;
        String termoBusca;
        String dtmParaSalvar;

        XmlInfo(String content, String idMov, String tabela, String termo) {
            this.xmlContent = content;
            this.idMovimento = idMov;
            this.tabelaFonte = tabela;
            this.termoBusca = termo;
        }
    }

    public static void main(String[] args) {
        
        String chaveCteParaBuscar = "44443333222211110000999988887777666655554444";

        if (chaveCteParaBuscar.length() != 44 || !chaveCteParaBuscar.matches("\\d+")) {
            System.err.println("ERRO: A variável 'chaveCteParaBuscar' não parece ser uma chave CTe de 44 dígitos válida.");
            System.err.println("Por favor, edite o código e tente novamente.");
            return;
        }
        
        System.out.println("--- Busca de XML por Chave CTe ---");
        System.out.println(String.format("Procurando chave: %s...", chaveCteParaBuscar.substring(0, 6) + "..."));
        System.out.println(String.format("Alvo: %s.%s", DATABASE_BUSCA, TABELA_BUSCA));

        String connectionUrl = String.format(
                "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;",
                SQLSERVER_SERVER, SQLSERVER_PORT, DATABASE_BUSCA
        );
        
        try (Connection conn = DriverManager.getConnection(connectionUrl, SQLSERVER_USER, SQLSERVER_PASSWORD)) {
            
            System.out.println("Conexão estabelecida. Buscando...");
            
            XmlInfo xmlEncontrado = buscarXmlEmTabelaUnica(conn, chaveCteParaBuscar, TABELA_BUSCA, DATABASE_BUSCA);
            
            if (xmlEncontrado != null) {
                System.out.println(String.format("SUCESSO! XML encontrado (ID Movimento: %s).", xmlEncontrado.idMovimento));
                
                xmlEncontrado.dtmParaSalvar = chaveCteParaBuscar; 
                
                salvarArquivo(xmlEncontrado);
            } else {
                System.err.println("\n--- RESULTADO: Chave não encontrada na tabela " + TABELA_BUSCA + " ---");
            }
            
        } catch (SQLException e) {
            String msgErro = e.getMessage().toLowerCase();
            if (msgErro.contains("invalid object name") || msgErro.contains("objeto inválido")) {
                System.err.println("ERRO CRÍTICO: A tabela " + TABELA_BUSCA + " não existe no banco " + DATABASE_BUSCA);
            } else if (msgErro.contains("login failed")) {
                System.err.println("ERRO CRÍTICO: Falha no login. Verifique usuário e senha.");
            } else {
                System.err.println("ERRO CRÍTICO de Conexão ou SQL: " + e.getMessage());
            }
        }
        
        System.out.println("Busca finalizada.");
    }

    private static XmlInfo buscarXmlEmTabelaUnica(Connection conn, String termoBusca, String nomeTabela, String dbUsado) throws SQLException {
        String termoLimpo = termoBusca.replace("\"", "").trim();
        String termoLike = "%" + termoLimpo + "%";
        
        String sqlLike = String.format("SELECT TOP 1 ds_XML, id_Movimento FROM %s WITH (NOLOCK) WHERE ds_XML LIKE ?", nomeTabela);

        try (PreparedStatement pstmt = conn.prepareStatement(sqlLike)) {
            pstmt.setString(1, termoLike); 
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String idMovimentoEncontrado = rs.getString("id_Movimento");
                    return new XmlInfo(rs.getString("ds_XML"), idMovimentoEncontrado, dbUsado + "." + nomeTabela, termoBusca);
                }
            }
        } catch (SQLException e) {
            throw e; 
        }
        
        return null; 
    }

    private static void salvarArquivo(XmlInfo xmlInfo) { 
        String xmlContent = xmlInfo.xmlContent;
        if (xmlContent == null || xmlContent.isEmpty()) {
            System.err.println("   -> AVISO: Tentativa de salvar XML vazio para o termo: " + xmlInfo.termoBusca);
            return;
        }
        
        if (xmlInfo.dtmParaSalvar == null || xmlInfo.dtmParaSalvar.isEmpty()) {
            System.err.println("   -> ERRO FATAL: dtmParaSalvar (chave) está nula. Termo de busca: " + xmlInfo.termoBusca);
            return;
        }

        try {
            String chaveCteExtraida = extrairChaveDoXml(xmlContent);
            String nomeBasePasta = xmlInfo.dtmParaSalvar.trim();
            String nomePasta = nomeBasePasta;
            String nomeArquivo;

            if (chaveCteExtraida != null) {
                nomeArquivo = nomeBasePasta + "_" + chaveCteExtraida + ".xml";
            } else {
                nomeArquivo = nomeBasePasta + "_semChaveNoXml.xml";
                System.err.println("   -> AVISO: Chave CTe não extraída do XML.");
            }

            String diretorioDestino = PASTA_RAIZ_DOWNLOAD + nomePasta + File.separator; 

            File diretorio = new File(diretorioDestino);
            if (!diretorio.exists()) {
                if (!diretorio.mkdirs()) {
                    System.err.println("   -> ERRO AO CRIAR PASTA: Não foi possível criar " + diretorioDestino);
                    return;
                }
            }

            String caminhoCompleto = diretorioDestino + nomeArquivo;
            
            File f = new File(caminhoCompleto);
            if (f.exists()) {
                System.out.println("   -> AVISO: Arquivo já existe, pulando o salvamento. Caminho: " + caminhoCompleto);
                return;
            }

            try (FileWriter writer = new FileWriter(caminhoCompleto)) {
                writer.write(xmlContent);
                System.out.println("   -> XML salvo em: " + caminhoCompleto);
            } catch (IOException e) {
                System.err.println("   -> ERRO AO SALVAR ARQUIVO: " + e.getMessage() + " | Caminho: " + caminhoCompleto);
            }
        } catch (Exception e) {
            System.err.println(" 	 -> ERRO GERAL AO SALVAR ARQUIVO para termo " + xmlInfo.termoBusca + ": " + e.getMessage());
        }
    }

    private static String extrairChaveDoXml(String xmlContent) {
        if (xmlContent == null || xmlContent.isEmpty()) return null;
        try {
            Pattern patternId = Pattern.compile("Id=\"CTe(\\d{44})\"");
            Matcher matcherId = patternId.matcher(xmlContent);
            if (matcherId.find()) {
                return matcherId.group(1);
            }
            Pattern patternCh = Pattern.compile("<chCTe>(\\d{44})</chCTe>");
            Matcher matcherCh = patternCh.matcher(xmlContent);
            if (matcherCh.find()) {
                return matcherCh.group(1);
            }
        } catch (Exception e) {
            System.err.println("   -> ERRO (interno): Falha ao extrair chave CTe do XML. Detalhes: " + e.getMessage());
        }
        return null;
    }
}