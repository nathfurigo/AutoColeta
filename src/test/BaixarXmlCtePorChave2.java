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
import java.util.List;

/**
 * Ferramenta autônoma (monolito) para baixar múltiplos XMLs de CTe em lote
 * diretamente do banco de dados SQL Server e salvá-los em pastas específicas.
 * Lógica de busca ajustada para usar o id_Movimento.
 */
public class BaixarXmlCtePorChave2 {

    // --- CONFIGURAÇÕES DO BANCO DE DADOS ---
    private static final String DB_SERVER = "192.168.10.251";
    private static final String DB_PORT = "1433";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "esl@13509";
    // --- FIM DAS CONFIGURAÇÕES ---

    /**
     * Ponto de entrada principal para a execução da ferramenta.
     */
    public static void main(String[] args) {
        System.out.println("--- Ferramenta de Download de XML do CTe em Lote (Busca por ID Movimento) ---");

        List<String> dtmsNaoEncontrados = new ArrayList<>();

        // <<< MUDANÇA PRINCIPAL 1: O array agora tem 3 colunas: { "DTM", "ID_Movimento", "Chave de Acesso" } >>>
        // Itens que já foram processados com sucesso foram removidos.
        String[][] dadosParaBusca = {
                {"200083470", "995286", "35241207890229000198570080000015651498452285"},
                {"200082824", "993737", "35241207890229000198570070000005861965497539"},
                {"200083155", "994814", "35241207890229000198570080000010581391243679"},
                {"200082977", "995014", "35241207890229000198570080000012701231732146"},
                {"200083359", "995306", "35241207890229000198570080000015851547203541"},
                {"200082227", "995082", "35241207890229000198570080000013381027511992"},
                {"200083116", "995144", "35241207890229000198570080000014031615387389"},
                {"200082318", "995078", "35241207890229000198570080000013341575446534"},
                {"200079870", "987013", "35241107890229000198570080000004031640301141"},
                {"200081970", "994812", "35241207890229000198570080000010561807437939"},
                {"200084168", "995290", "35241207890229000198570080000015691059156214"},
                {"200080937", "986995", "35241107890229000198570080000003851307528749"},
                {"200082188", "994947", "35241207890229000198570080000011971186490779"},
                {"200081807", "994421", "35241207890229000198570080000006441114988320"},
                {"200084715", "995552", "35241207890229000198570080000018471266630352"},
                {"200081484", "994422", "35241207890229000198570080000006451566618501"},
                {"200080654", "987012", "35241107890229000198570080000004021582103551"},
                {"200079958", "986988", "35241107890229000198570080000003771414659713"},
                {"200080658", "987011", "35241107890229000198570080000004011425915462"},
                {"200080598", "986891", "35241107890229000198570080000002731733570766"},
                {"200081790", "994420", "35241207890229000198570080000006431623716649"},
                {"200079808", "986835", "35241107890229000198570080000002161767518264"},
                {"200083635", "995125", "35241207890229000198570080000013831880796190"},
                {"200079559", "986868", "35241107890229000198570080000002501718773967"},
                {"200080336", "987065", "35241107890229000198570080000004581695375005"},
                {"200079637", "986711", "35241107890229000198570080000000741139310117"},
                {"200083731", "995284", "35241207890229000198570080000015631386119148"},
                {"200079099", "986880", "35241107890229000198570080000002621352822170"},
                {"200083556", "993923", "35241207890229000198570070000008051493549091"},
                {"200077579", "987016", "35241107890229000198570080000004061342099044"},
                {"200077579", "988001", "35241160541240000125570100000000171105418268"},
                {"200083499", "994969", "35241207890229000198570080000012231600501310"},
                {"200083479", "995288", "35241207890229000198570080000015671001288318"},
                {"200083141", "994813", "35241207890229000198570080000010571901281286"}
        };
        
        for (String[] dado : dadosParaBusca) {
            String valorBuscado = dado[0];
            String idMovimento = dado[1];
            String chaveAcesso = dado[2];
            
            System.out.println("\n" + "-".repeat(60));
            System.out.println("Processando DTM: " + valorBuscado + " | ID Movimento: " + idMovimento);

            String diretorioBase = "C:\\DTM Weliton\\";
            String caminhoCompletoArquivo = diretorioBase + valorBuscado + File.separator + chaveAcesso + ".xml";
            File arquivoXml = new File(caminhoCompletoArquivo);

            if (arquivoXml.exists()) {
                System.out.println("INFO: O arquivo XML já existe. Pulando para o próximo.");
                continue; 
            }
            
            if (chaveAcesso.length() == 44 && chaveAcesso.matches("\\d+")) {
                boolean sucesso = buscarESalvarXml(chaveAcesso, valorBuscado, idMovimento);
                if (!sucesso) {
                    dtmsNaoEncontrados.add("DTM: " + valorBuscado + " (ID Movimento: " + idMovimento + ", Chave: " + chaveAcesso + ")");
                }
            } else {
                System.err.println("Erro: A chave de acesso '" + chaveAcesso + "' é inválida. Pulando...");
            }
        }
        
        System.out.println("\n" + "-".repeat(60));
        System.out.println("--- Processo em lote finalizado! ---");

        if (!dtmsNaoEncontrados.isEmpty()) {
            System.err.println("\n--- RESUMO: XMLs NÃO ENCONTRADOS NO BANCO DE DADOS ---");
            for (String dtm : dtmsNaoEncontrados) {
                System.err.println("- " + dtm);
            }
        } else {
            System.out.println("\n--- RESUMO: Todos os XMLs foram encontrados e processados com sucesso! ---");
        }
    }

    /**
     * Conecta ao banco de dados, busca o XML e o salva em um arquivo.
     * @param chaveCte A chave de acesso de 44 dígitos (usada para definir a tabela).
     * @param pastaDestino O nome da pasta onde o XML será salvo (o "Valor Buscado").
     * @param idMovimento O ID do movimento a ser buscado na tabela.
     * @return Retorna true se encontrou o XML, false caso contrário.
     */
    private static boolean buscarESalvarXml(String chaveCte, String pastaDestino, String idMovimento) {
        System.out.println("--- Iniciando busca do XML no banco de dados ---");

        try {
            String ano = chaveCte.substring(2, 4);
            String mes = chaveCte.substring(4, 6);
            
            String nomeDatabase = "dtbCTe20" + ano;
            String nomeTabela = String.format("[%s].[dbo].[tbdCTeXMLMovimento%s]", nomeDatabase, mes);

            String connectionUrl = String.format(
                "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;",
                DB_SERVER, DB_PORT, nomeDatabase
            );
            
            // <<< MUDANÇA PRINCIPAL 2: A query agora busca pelo id_Movimento >>>
            String sqlQuery = String.format("SELECT TOP 1 ds_XML FROM %s WHERE id_Movimento = ?", nomeTabela);

            System.out.println("1. Conectando ao banco de dados: '" + nomeDatabase + "'...");

            try (Connection conn = DriverManager.getConnection(connectionUrl, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sqlQuery)) {
                
                // <<< MUDANÇA PRINCIPAL 3: O parâmetro da query agora é o idMovimento >>>
                pstmt.setString(1, idMovimento);
                System.out.println("2. Executando consulta na tabela: " + nomeTabela + " com id_Movimento = " + idMovimento);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("3. Registro encontrado!");
                        String xmlContent = rs.getString("ds_XML");
                        salvarArquivo(chaveCte, xmlContent, pastaDestino);
                        return true;
                    } else {
                        System.err.println("AVISO: Nenhum XML foi encontrado para o id_Movimento fornecido nesta tabela.");
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("ERRO DE BANCO DE DADOS: Não foi possível conectar ou executar a consulta.");
            System.err.println("Verifique se a VPN está ativa, se os dados de conexão estão corretos e se a tabela/banco para o ano/mês existe.");
            return false;
        } catch (Exception e) {
            System.err.println("Ocorreu um erro inesperado: " + e.getMessage());
            return false;
        }
    }

    /**
     * Salva o conteúdo XML em um arquivo no diretório especificado.
     */
    private static void salvarArquivo(String nomeBase, String conteudo, String nomePasta) {
        try {
            String diretorioBase = "C:\\DTM Weliton\\";
            String diretorioDestino = diretorioBase + nomePasta + File.separator;
            
            File diretorio = new File(diretorioDestino);
            if (!diretorio.exists()) {
                System.out.println("INFO: Criando pasta em: " + diretorioDestino);
                diretorio.mkdirs();
            }

            String caminhoCompletoArquivo = diretorioDestino + nomeBase + ".xml";

            try (FileWriter writer = new FileWriter(caminhoCompletoArquivo)) {
                System.out.println("4. Salvando o conteúdo no arquivo: " + caminhoCompletoArquivo);
                writer.write(conteudo);
                System.out.println("--- SUCESSO! XML salvo. ---");
            } catch (IOException e) {
                System.err.println("ERRO: Não foi possível salvar o arquivo. Verifique as permissões de escrita na pasta " + diretorioDestino);
            }
        } catch (Exception e) {
            System.err.println("ERRO ao tentar salvar o arquivo: " + e.getMessage());
        }
    }
}