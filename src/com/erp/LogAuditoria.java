package com.erp;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogAuditoria {

    private static final String ARQUIVO_LOG = "auditoria.log";

    public static void registrar(String acao, String detalhe) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String linha = "[" + timestamp + "] " + acao + " - " + detalhe;

        try (PrintWriter writer = new PrintWriter(new FileWriter(ARQUIVO_LOG, true))) {
            writer.println(linha);
        } catch (IOException e) {
            System.out.println("Erro ao gravar log de auditoria: " + e.getMessage());
        }
    }
}