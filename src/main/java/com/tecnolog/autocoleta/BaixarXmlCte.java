package com.tecnolog.autocoleta;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Ferramenta autônoma (monolito) para baixar XML de CTe diretamente do banco de dados SQL Server.
 * Esta classe pode ser executada de forma independente via Maven para realizar a tarefa.
 */
public class BaixarXmlCte {

    // --- CONFIGURAÇÕES DO BANCO DE DADOS ---
    // Extraído do seu arquivo application-prod.yml
    private static final String DB_SERVER = "192.168.10.251";
    private static final String DB_PORT = "1433";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "esl@13509";
    // --- FIM DAS CONFIGURAÇÕES ---

    /**
     * Ponto de entrada principal para a execução da ferramenta.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("--- Ferramenta de Download de XML do CTe (Java) ---");

        while (true) {
            System.out.print("\nPor favor, cole a chave de acesso do CTe (44 dígitos) ou digite 'sair' para fechar: ");
            String chaveAcesso = scanner.nextLine().trim();

            if (chaveAcesso.equalsIgnoreCase("sair")) {
                System.out.println("Encerrando o programa.");
                break;
            }

            if (chaveAcesso.length() == 44 && chaveAcesso.matches("\\d+")) {
                buscarESalvarXml(chaveAcesso);
            } else {
                System.err.println("Erro: A chave de acesso deve conter exatamente 44 dígitos numéricos. Tente novamente.");
            }
            System.out.println("-".repeat(50));
        }
        scanner.close();
    }

    /**
     * Conecta ao banco de dados, busca o XML e o salva em um arquivo.
     * @param chaveCte A chave de acesso de 44 dígitos.
     */
    private static void buscarESalvarXml(String chaveCte) {
        System.out.println("\n--- Iniciando busca do XML no banco de dados ---");

        try {
            // Extrai o Ano e o Mês da chave para montar os nomes dinamicamente
            String ano = chaveCte.substring(2, 4);
            String mes = chaveCte.substring(4, 6);
            
            String nomeDatabase = "dtbCTe20" + ano;
            String nomeTabela = String.format("[%s].[dbo].[tbdCTeXMLMovimento%s]", nomeDatabase, mes);

            String connectionUrl = String.format(
                "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;",
                DB_SERVER, DB_PORT, nomeDatabase
            );

            // Query CORRIGIDA: Busca pela chave completa dentro da coluna ds_XML
            String sqlQuery = String.format("SELECT TOP 1 ds_XML FROM %s WHERE ds_XML LIKE ?", nomeTabela);

            System.out.println("1. Conectando ao banco de dados: '" + nomeDatabase + "'...");

            try (Connection conn = DriverManager.getConnection(connectionUrl, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sqlQuery)) {
                
                pstmt.setString(1, "%" + chaveCte + "%");
                System.out.println("2. Executando consulta na tabela: " + nomeTabela);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("3. Registro encontrado!");
                        String xmlContent = rs.getString("ds_XML");
                        salvarArquivo(chaveCte, xmlContent);
                    } else {
                        System.err.println("AVISO: Nenhum XML foi encontrado para a chave de acesso fornecida nesta tabela.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERRO DE BANCO DE DADOS: Não foi possível conectar ou executar a consulta.");
            System.err.println("Verifique se a VPN está ativa, se os dados de conexão estão corretos e se a tabela/banco para o ano/mês existe.");
            // e.printStackTrace(); // Descomente para ver o erro completo
        } catch (Exception e) {
            System.err.println("Ocorreu um erro inesperado: " + e.getMessage());
        }
    }

    /**
     * Salva o conteúdo XML em um arquivo no diretório especificado.
     * @param nomeBase Nome do arquivo (sem extensão), geralmente a chave do CTe.
     * @param conteudo Conteúdo XML a ser salvo.
     */
    private static void salvarArquivo(String nomeBase, String conteudo) {
        // Define o diretório de destino fixo.
        String diretorioDestino = "C:\\DTM Weliton\\XML\\";
        
        // Garante que o diretório exista. Se não existir, ele será criado.
        java.io.File diretorio = new java.io.File(diretorioDestino);
        if (!diretorio.exists()) {
            System.out.println("INFO: O diretório de destino não existe. Criando pasta em: " + diretorioDestino);
            diretorio.mkdirs(); // Cria o diretório e qualquer pasta pai necessária.
        }

        // Monta o caminho completo do arquivo.
        String nomeArquivo = diretorioDestino + nomeBase + ".xml";

        try (FileWriter writer = new FileWriter(nomeArquivo)) {
            System.out.println("4. Salvando o conteúdo no arquivo: " + nomeArquivo);
            writer.write(conteudo);
            System.out.println("\n--- SUCESSO! ---");
            System.out.println("O arquivo XML foi salvo com sucesso em: " + diretorioDestino);
        } catch (IOException e) {
            System.err.println("ERRO: Não foi possível salvar o arquivo. Verifique as permissões de escrita na pasta " + diretorioDestino);
            // e.printStackTrace(); // Descomente para ver o erro completo
        }
    }
}
