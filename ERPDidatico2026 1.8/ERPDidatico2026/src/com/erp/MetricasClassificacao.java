package com.erp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MetricasClassificacao {

    private static final String ARQUIVO = "classificacao_log.txt";
    private static final double ACEITACAO_MINIMA = 0.70;
    private static final int MINIMO_PARA_CONCLUIR = 20;

    public static void registrar(String tipo, String identificador, String sugerida,
                                 double confianca, String escolhida) {
        String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        boolean aceita = sugerida.equals(escolhida);
        String linha = String.format(java.util.Locale.US, "%s,%s,%s,%s,%.3f,%s,%s",
                data, tipo, identificador, sugerida, confianca, escolhida,
                aceita ? "ACEITA" : "CORRIGIDA");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO, true))) {
            writer.write(linha);
            writer.newLine();
        } catch (IOException e) {
            Console.aviso(Idioma.t("ia.metrica.gravar"));
        }
    }

    public static void registrarFalha(String tipo, String identificador, String motivo) {
        String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO, true))) {
            writer.write(String.format("%s,%s,%s,-,0.000,-,FALHA(%s)",
                    data, tipo, identificador, motivo));
            writer.newLine();
        } catch (IOException e) {
        }
    }

    private static class Contador {
        int aceitas;
        int corrigidas;

        int total() {
            return aceitas + corrigidas;
        }

        double taxa() {
            return total() == 0 ? 0 : (double) aceitas / total();
        }
    }

    public static void relatorio() {
        List<String[]> linhas = lerLinhas();
        Console.titulo(Idioma.t("ia.relatorio.titulo"));

        if (linhas.isEmpty()) {
            System.out.println(Idioma.t("ia.relatorio.vazio"));
            return;
        }

        Contador geral = new Contador();
        Map<String, Contador> porCategoria = new LinkedHashMap<>();
        int falhas = 0;
        double somaConfiancaAceita = 0;
        double somaConfiancaCorrigida = 0;

        for (String[] p : linhas) {
            String situacao = p[6];
            if (situacao.startsWith("FALHA")) {
                falhas++;
                continue;
            }
            boolean aceita = situacao.equals("ACEITA");
            double confianca = Double.parseDouble(p[4]);
            Contador categoria = porCategoria.computeIfAbsent(p[3], k -> new Contador());
            if (aceita) {
                geral.aceitas++;
                categoria.aceitas++;
                somaConfiancaAceita += confianca;
            } else {
                geral.corrigidas++;
                categoria.corrigidas++;
                somaConfiancaCorrigida += confianca;
            }
        }

        int chamadas = geral.total() + falhas;
        System.out.printf("%-34s %s%n", Idioma.t("ia.relatorio.chamadas"), chamadas);
        System.out.printf("%-34s %s%n", Idioma.t("ia.relatorio.aceitas"), geral.aceitas);
        System.out.printf("%-34s %s%n", Idioma.t("ia.relatorio.corrigidas"), geral.corrigidas);
        System.out.printf("%-34s %s%n", Idioma.t("ia.relatorio.falhas"), falhas);
        if (chamadas > 0) {
            System.out.printf("%-34s %s%n", Idioma.t("ia.relatorio.disponibilidade"),
                    Formato.numero(100.0 * (chamadas - falhas) / chamadas, 1) + "%");
        }
        if (geral.total() > 0) {
            System.out.printf("%-34s %s%n", Idioma.t("ia.relatorio.aceitacao"),
                    Formato.numero(geral.taxa() * 100, 1) + "%");
        }

        if (geral.aceitas > 0 && geral.corrigidas > 0) {
            System.out.println();
            System.out.printf("%-34s %s%n", Idioma.t("ia.relatorio.conf.aceitas"),
                    Formato.numero(100 * somaConfiancaAceita / geral.aceitas, 1) + "%");
            System.out.printf("%-34s %s%n", Idioma.t("ia.relatorio.conf.corrigidas"),
                    Formato.numero(100 * somaConfiancaCorrigida / geral.corrigidas, 1) + "%");
        }

        if (!porCategoria.isEmpty()) {
            System.out.println();
            Console.linha();
            System.out.printf("%-20s %10s %10s %12s%n", Idioma.t("ia.relatorio.col.sugestao"),
                    Idioma.t("ia.relatorio.col.aceitas"), Idioma.t("ia.relatorio.col.corrigidas"),
                    Idioma.t("ia.relatorio.col.taxa"));
            Console.linha();
            for (Map.Entry<String, Contador> e : porCategoria.entrySet()) {
                Contador c = e.getValue();
                System.out.printf("%-20s %10d %10d %11s%%%n", e.getKey(), c.aceitas, c.corrigidas,
                        Formato.numero(c.taxa() * 100, 1));
            }
            Console.linha();
        }

        System.out.println();
        if (geral.total() < MINIMO_PARA_CONCLUIR) {
            Console.aviso(Idioma.t("ia.relatorio.poucos", MINIMO_PARA_CONCLUIR));
        } else if (geral.taxa() < ACEITACAO_MINIMA) {
            Console.aviso(Idioma.t("ia.relatorio.retreinar",
                    Formato.numero(ACEITACAO_MINIMA * 100, 0)));
        } else {
            Console.ok(Idioma.t("ia.relatorio.saudavel",
                    Formato.numero(ACEITACAO_MINIMA * 100, 0)));
        }
    }

    private static List<String[]> lerLinhas() {
        List<String[]> linhas = new ArrayList<>();
        Path caminho = Path.of(ARQUIVO);
        if (!Files.exists(caminho)) {
            return linhas;
        }
        try {
            for (String linha : Files.readAllLines(caminho, StandardCharsets.UTF_8)) {
                String[] p = linha.split(",");
                if (p.length >= 7) {
                    linhas.add(p);
                }
            }
        } catch (IOException e) {
            Console.aviso(Idioma.t("ia.metrica.ler", ARQUIVO));
        }
        return linhas;
    }
}