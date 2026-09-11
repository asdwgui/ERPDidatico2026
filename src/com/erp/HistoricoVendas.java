package com.erp;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class HistoricoVendas {

    private static final String ARQUIVO_HISTORICO = "historico_vendas.txt";

    public static void registrarVenda(String produtoId) {
        String data = LocalDate.now().toString(); // formato: yyyy-MM-dd
        String linha = produtoId + "," + data;

        try (PrintWriter writer = new PrintWriter(new FileWriter(ARQUIVO_HISTORICO, true))) {
            writer.println(linha);
        } catch (IOException e) {
            System.out.println("Erro ao registrar histórico de vendas: " + e.getMessage());
        }
    }

    public static List<String[]> carregarHistorico() throws IOException {
        List<String[]> registros = new ArrayList<>();
        File file = new File(ARQUIVO_HISTORICO);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String linha;
                while ((linha = reader.readLine()) != null) {
                    if (!linha.isBlank()) {
                        registros.add(linha.split(","));
                    }
                }
            }
        }
        return registros;
    }
}