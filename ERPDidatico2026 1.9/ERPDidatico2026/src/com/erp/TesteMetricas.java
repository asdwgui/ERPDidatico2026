package com.erp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
// DESAFIO 10 - Etapa 1: mede o tempo de resposta das operações mais comuns do ERP.
// Rode UMA vez com os dados normais, anote, rode o GeradorMassa e rode de novo:
// as duas execuções lado a lado são a tabela "antes e depois" do relatório.
public class TesteMetricas {
    private static final int REPETICOES = 20;
    private static final int BUSCAS = 10000;
    public static void main(String[] args) throws IOException {
        long inicio = System.nanoTime();
        Estoque estoque = new Estoque();
        double msAbrir = decorrido(inicio);
        List<Produto> produtos = estoque.getProdutos();
// 1) Consulta de estoque por nome - a operação que o usuário mais repete
        for (int i = 0; i < 5; i++) {
            TelaConsultaProdutos.filtrar(produtos, "notebook dell"); // aquecimento
        }
        double msConsulta = 0;
        int encontrados = 0;
        for (int i = 0; i < REPETICOES; i++) {
            long t = System.nanoTime();
            encontrados = TelaConsultaProdutos.filtrar(produtos, "notebook dell").size();
            msConsulta += decorrido(t);
        }
        msConsulta = msConsulta / REPETICOES;
        // 2) Busca de um produto pelo ID - usada em toda venda e toda compra
        Random sorteio = new Random(5);
        String[] ids = new String[BUSCAS];
        for (int i = 0; i < BUSCAS; i++) {
            ids[i] = produtos.get(sorteio.nextInt(produtos.size())).getId();
        }
        long t2 = System.nanoTime();
        int achados = 0;
        for (String id : ids) {
            if (estoque.buscarProdutoPorId(id) != null) {
                achados++;
            }
        }
        double msBuscas = decorrido(t2);
        // 3) Relatório de inventário: primeira chamada (calcula) e segunda (cache)
        double msRelatorio1 = medirRelatorio(estoque);
        double msRelatorio2 = medirRelatorio(estoque);
        // 4) Previsão de demanda: lê o arquivo de histórico inteiro
        double msPrevisao = medirPrevisao(estoque, produtos);
        Console.linha();
        System.out.println("MÉTRICAS DO ERP - " + produtos.size() + " produtos cadastrados");
        Console.linha();
        System.out.printf("%-46s %10.1f ms%n", "Abrir o ERP (carregar todos os arquivos)", msAbrir);
        System.out.printf("%-46s %10.1f ms%n", "Consulta de estoque por nome (média de " + REPETICOES + ")",
                msConsulta);
        System.out.printf("%-46s %10.1f ms%n", BUSCAS + " buscas por ID (índice em memória)", msBuscas);
        System.out.printf("%-46s %10.1f ms%n", "Relatório de inventário (1ª vez, calcula)", msRelatorio1);
        System.out.printf("%-46s %10.1f ms%n", "Relatório de inventário (2ª vez, cache)", msRelatorio2);
        System.out.printf("%-46s %10.1f ms%n", "Previsão de demanda (lê o histórico)", msPrevisao);
        Console.linha();
        System.out.println("conferência: " + encontrados + " produtos no filtro, " + achados + " IDs encontrados");
    }

    private static double medirRelatorio(Estoque estoque) {
        PrintStream original = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        long t = System.nanoTime();
        try {
            estoque.relatorioInventario();
        } finally {
            System.setOut(original);
        }
        return decorrido(t);
    }
    private static double medirPrevisao(Estoque estoque, List<Produto> produtos) throws IOException {
        String id = produtos.get(produtos.size() - 1).getId();
        PrintStream original = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        long t = System.nanoTime();
        try {
            estoque.preverDemanda(new Scanner(id + System.lineSeparator()));
        } finally {
            System.setOut(original);
        }
        return decorrido(t);
    }
    private static double decorrido(long inicioNano) {
        return (System.nanoTime() - inicioNano) / 1_000_000.0;
    }
}