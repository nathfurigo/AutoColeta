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
 */
public class BaixarXmlCte {

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
        System.out.println("--- Ferramenta de Download de XML do CTe em Lote (Java) ---");

        List<String> dtmsNaoEncontrados = new ArrayList<>();

        String[][] dadosParaBusca = {
            // Lista Original
            {"200086138", "35250107890229000198570080000025691424994199"},
            {"200083470", "35241207890229000198570080000015651498452285"},
            {"200086944", "35250107890229000198570080000026341217753854"},
            {"200082824", "35241207890229000198570070000005861965497539"},
            {"200084894", "35250160541240000125570100000001151494586507"},
            {"200084894", "35250107890229000198570080000020141792026585"},
            {"200083155", "35241207890229000198570080000010581391243679"},
            {"200083458", "35250107890229000198570080000026661821368530"},
            {"200086992", "35250107890229000198570080000026021606213121"},
            {"200087410", "35250107890229000198570080000027161333611855"},
            {"200082977", "35241207890229000198570080000012701231732146"},
            {"200085906", "35250107890229000198570080000025311583973946"},
            {"200083359", "35241207890229000198570080000015851547203541"},
            {"200087158", "35250107890229000198570080000025991881937235"},
            {"200082227", "35241207890229000198570080000013381027511992"},
            {"200087425", "35250160541240000125570100000001921797420396"},
            {"200087425", "35250107890229000198570080000027281214118986"},
            {"200083116", "35241207890229000198570080000014031615387389"},
            {"200087435", "35250107890229000198570080000027131842390944"},
            {"200085633", "35250107890229000198570080000021521941151528"},
            {"200082318", "35241207890229000198570080000013341575446534"},
            {"200086575", "35250107890229000198570080000025671692757546"},
            {"200079870", "35241107890229000198570080000004031640301141"},
            {"200085622", "35250107890229000198570080000021511652586390"},
            {"200081970", "35241207890229000198570080000010561807437939"},
            {"200084168", "35241207890229000198570080000015691059156214"},
            {"200080937", "35241107890229000198570080000003851307528749"},
            {"200087541", "35250107890229000198570080000026891298708160"},
            {"200082188", "35241207890229000198570080000011971186490779"},
            {"200087530", "35250107890229000198570080000026971801136029"},
            {"200081807", "35241207890229000198570080000006441114988320"},
            {"200084715", "35241207890229000198570080000018471266630352"},
            {"200085566", "35250160541240000125570090000023421976002726"},
            {"200085566", "35250107890229000198570070000014041349937005"},
            {"200086346", "35250107890229000198570080000025681047268180"},
            {"200081484", "35241207890229000198570080000006451566618501"},
            {"200080654", "35241107890229000198570080000004021582103551"},
            {"200087631", "35250107890229000198570080000027151740975654"},
            {"200079958", "35241107890229000198570080000003771414659713"},
            {"200086939", "35250107890229000198570070000017241703043490"},
            {"200086502", "35250107890229000198570080000025811581871340"},
            {"200080658", "35241107890229000198570080000004011425915462"},
            {"200080598", "35241107890229000198570080000002731733570766"},
            {"200086148", "35250160541240000125570090000023411873389264"},
            {"200086148", "35250107890229000198570070000014071022677459"},
            {"200081790", "35241207890229000198570080000006431623716649"},
            {"200079808", "35241107890229000198570080000002161767518264"},
            {"200083635", "35241207890229000198570080000013831880796190"},
            {"200079559", "35241107890229000198570080000002501718773967"},
            {"200086136", "35250107890229000198570080000025701032502066"},
            {"200080336", "35241107890229000198570080000004581695375005"},
            {"200087111", "35250107890229000198570080000026601810063870"},
            {"200087561", "35250107890229000198570080000026921631508331"},
            {"200086918", "35250107890229000198570080000026031800829571"},
            {"200087387", "35250107890229000198570080000027121762796190"},
            {"200079637", "35241107890229000198570080000000741139310117"},
            {"200087365", "35250107890229000198570080000027101949528526"},
            {"200083731", "35241207890229000198570080000015631386119148"},
            {"200079099", "35241107890229000198570080000002621352822170"},
            {"200087516", "35250107890229000198570080000027071914098562"},
            {"200083556", "35241207890229000198570070000008051493549091"},
            {"200077579", "35241107890229000198570080000004061342099044"},
            {"200077579", "35241160541240000125570100000000171105418268"},
            {"200087195", "35250107890229000198570080000026871843654226"},
            {"200083499", "35241207890229000198570080000012231600501310"},
            {"200086474", "35250107890229000198570080000025841541803116"},
            {"200083479", "35241207890229000198570080000015671001288318"},
            {"200085626", "35250107890229000198570080000021681919748363"},
            {"200083141", "35241207890229000198570080000010571901281286"},

            // >>> INÍCIO DA MODIFICAÇÃO: Novos CT-es adicionados <<<
            {"200079099", "35241107890229000198570080000002621352822170"},
            {"200083731", "35241207890229000198570080000015631386119148"},
            {"200083155", "35241207890229000198570080000010581391243679"},
            {"200079870", "35241107890229000198570080000004031640301141"},
            {"200080336", "35241107890229000198570080000004581695375005"},
            {"200082977", "35241207890229000198570080000012701231732146"},
            {"200079637", "35241107890229000198570080000000741139310117"},
            {"200081807", "35241207890229000198570080000006441114988320"},
            {"200082227", "35241207890229000198570080000013381027511992"},
            {"200080598", "35241107890229000198570080000002731733570766"},
            {"200082188", "35241207890229000198570080000011971186490779"},
            {"200080654", "35241107890229000198570080000004021582103551"},
            {"200077579", "35241107890229000198570080000004061342099044"},
            {"200077579", "35241160541240000125570100000000171105418268"},
            {"200083116", "35241207890229000198570080000014031615387389"},
            {"200084715", "35241207890229000198570080000018471266630352"},
            {"200083479", "35241207890229000198570080000015671001288318"},
            {"200083141", "35241207890229000198570080000010571901281286"},
            {"200079808", "35241107890229000198570080000002161767518264"},
            {"200082824", "35241207890229000198570070000005861965497539"},
            {"200083556", "35241207890229000198570070000008051493549091"},
            {"200083470", "35241207890229000198570080000015651498452285"},
            {"200081484", "35241207890229000198570080000006451566618501"},
            {"200080937", "35241107890229000198570080000003851307528749"},
            {"200080658", "35241107890229000198570080000004011425915462"},
            {"200083359", "35241207890229000198570080000015851547203541"},
            {"200079559", "35241107890229000198570080000002501718773967"},
            {"200084168", "35241207890229000198570080000015691059156214"},
            {"200082318", "35241207890229000198570080000013341575446534"},
            {"200083635", "35241207890229000198570080000013831880796190"},
            {"200081790", "35241207890229000198570080000006431623716649"},
            {"200079958", "35241107890229000198570080000003771414659713"}
            // >>> FIM DA MODIFICAÇÃO <<<
        };
        
        for (String[] dado : dadosParaBusca) {
            String valorBuscado = dado[0];
            String chaveAcesso = dado[1];
            
            System.out.println("\n" + "-".repeat(60));
            System.out.println("Processando Valor Buscado: " + valorBuscado + " | Chave: " + chaveAcesso);

            String diretorioBase = "C:\\DTM Weliton\\";
            String caminhoCompletoArquivo = diretorioBase + valorBuscado + File.separator + chaveAcesso + ".xml";
            File arquivoXml = new File(caminhoCompletoArquivo);

            if (arquivoXml.exists()) {
                System.out.println("INFO: O arquivo XML já existe. Pulando para o próximo.");
                continue; 
            }
            
            if (chaveAcesso.length() == 44 && chaveAcesso.matches("\\d+")) {
                boolean sucesso = buscarESalvarXml(chaveAcesso, valorBuscado);
                if (!sucesso) {
                    dtmsNaoEncontrados.add(valorBuscado + " (Chave: " + chaveAcesso + ")");
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
     * @param chaveCte A chave de acesso de 44 dígitos.
     * @param pastaDestino O nome da pasta onde o XML será salvo (o "Valor Buscado").
     * @return Retorna true se encontrou o XML, false caso contrário.
     */
    private static boolean buscarESalvarXml(String chaveCte, String pastaDestino) {
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
                        salvarArquivo(chaveCte, xmlContent, pastaDestino);
                        return true;
                    } else {
                        System.err.println("AVISO: Nenhum XML foi encontrado para a chave de acesso fornecida nesta tabela.");
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