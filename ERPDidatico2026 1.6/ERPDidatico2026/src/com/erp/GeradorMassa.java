package com.erp;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class GeradorMassa {
    private static final String[] TIPOS = { "Notebook", "Celular", "Mouse", "Teclado", "Monitor",
            "Impressora", "Fone", "Carregador", "Cadeira", "Mesa", "Caneta", "Papel A4" };
    private static final String[] MARCAS = { "Dell", "Samsung", "LG", "HP", "Lenovo", "Multilaser", "Positivo"
    };
    public static void main(String[] args) throws IOException {
        int quantos = 5000;
        gerarProdutos(quantos);
        System.out.println(quantos + " produtos de teste adicionados ao produtos.txt.");
        System.out.println("Para voltar ao normal: apague do arquivo as linhas que comecam com T.");
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
// toPlainString() evita que o preço saia com vírgula e quebre o arquivo
                writer.write("T" + i + "," + nome + "," + preco.toPlainString() + "," + quantidade);
                writer.newLine();
            }
        }
    }
}