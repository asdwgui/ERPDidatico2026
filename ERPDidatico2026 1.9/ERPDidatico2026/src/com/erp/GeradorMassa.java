package com.erp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Random;

public class GeradorMassa {

    private static final String[] TIPOS = { "Notebook", "Celular", "Mouse", "Teclado", "Monitor",
            "Impressora", "Fone", "Carregador", "Cadeira", "Mesa", "Caneta", "Papel A4" };
    private static final String[] MARCAS = { "Dell", "Samsung", "LG", "HP", "Lenovo", "Multilaser", "Positivo"
    };

    private static final int PRODUTOS = 50000;
    private static final int HISTORICO = 500000;

    public static void main(String[] args) throws IOException {
        Medicao m1 = Medicao.iniciar("gerar " + PRODUTOS + " produtos");
        gerarProdutos(PRODUTOS);
        m1.encerrar();

        Medicao m2 = Medicao.iniciar("gerar " + HISTORICO + " linhas de histórico");
        gerarHistorico(HISTORICO, PRODUTOS);
        m2.encerrar();

        System.out.println(PRODUTOS + " produtos de teste adicionados ao produtos.txt.");
        System.out.println(HISTORICO + " linhas de histórico adicionadas ao historico_vendas.txt.");
        System.out.println("Para voltar ao normal: apague do produtos.txt as linhas que começam com T "
                + "e apague o historico_vendas.txt de teste.");
    }

    public static void gerarProdutos(int quantos) throws IOException {
        Random sorteio = new Random(7); // semente fixa: todo mundo da equipe gera os mesmos dados
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("produtos.txt", true))) {
            for (int i = 1; i <= quantos; i++) {
                String nome = TIPOS[sorteio.nextInt(TIPOS.length)] + " "
                        + MARCAS[sorteio.nextInt(MARCAS.length)] + " " + i;
                BigDecimal preco = BigDecimal.valueOf(20 + sorteio.nextInt(800000) / 100.0)
                        .setScale(2, RoundingMode.HALF_UP);
                int quantidade = sorteio.nextInt(300);
                writer.write("T" + i + "," + nome + "," + preco.toPlainString() + "," + quantidade);
                writer.newLine();
            }
        }
    }

    public static void gerarHistorico(int quantos, int quantosProdutos) throws IOException {
        Random sorteio = new Random(11);
        LocalDate hoje = LocalDate.now();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("historico_vendas.txt", true))) {
            for (int i = 0; i < quantos; i++) {
                String produtoId = "T" + (sorteio.nextInt(quantosProdutos) + 1);
                int quantidade = 1 + sorteio.nextInt(5);
                LocalDate data = hoje.minusDays(sorteio.nextInt(180));
                writer.write(produtoId + "," + quantidade + "," + data);
                writer.newLine();
            }
        }
    }
}