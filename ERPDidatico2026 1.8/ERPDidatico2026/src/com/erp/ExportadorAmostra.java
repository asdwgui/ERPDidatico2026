package com.erp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class ExportadorAmostra {

    public static final String ARQUIVO_PRODUTOS = "amostra_produtos.txt";
    public static final String ARQUIVO_CLIENTES = "amostra_clientes.txt";

    private static final int QUANTOS = 500;


    private static final String[] CATEGORIAS = {
            "Celular", "Notebook", "Monitor", "Periférico", "Mobiliário", "Papelaria" };

    private static final String[][] MODELOS = {
            { "Celular", "Smartphone", "Aparelho" },
            { "Notebook", "Laptop", "Ultrabook" },
            { "Monitor", "Tela", "Display" },
            { "Mouse", "Teclado", "Headset", "Webcam" },
            { "Cadeira", "Mesa", "Armário", "Gaveteiro" },
            { "Caneta", "Papel", "Caderno", "Grampeador" } };

    private static final String[] MARCAS = {
            "Dell", "Samsung", "LG", "HP", "Lenovo", "Multilaser", "Positivo", "Acer" };

    private static final int[][] FAIXA_PRECO = {
            { 800, 6000 },    // Celular
            { 2500, 9000 },   // Notebook
            { 600, 3500 },    // Monitor
            { 40, 900 },      // Periférico
            { 200, 2500 },    // Mobiliário
            { 3, 120 } };     // Papelaria

    private static final String[] SEGMENTOS = { "Varejo", "Atacado", "Corporativo" };

    public static void main(String[] args) throws IOException {
        int defeitosProduto = exportarProdutos();
        int defeitosCliente = exportarClientes();

        Console.linha();
        System.out.println("AMOSTRA EXPORTADA");
        Console.linha();
        System.out.printf("%-42s %6d registros%n", ARQUIVO_PRODUTOS, QUANTOS);
        System.out.printf("%-42s %6d registros%n", ARQUIVO_CLIENTES, QUANTOS);
        System.out.println();
        System.out.println("Registros com defeito plantados de propósito:");
        System.out.printf("%-42s %6d%n", "  produtos sem nome ou com preço zerado", defeitosProduto);
        System.out.printf("%-42s %6d%n", "  clientes duplicados", defeitosCliente);
        System.out.println();
    }

    private static int exportarProdutos() throws IOException {
        Random sorteio = new Random(42); // semente fixa: todo mundo gera a mesma amostra
        int defeitos = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO_PRODUTOS))) {
            writer.write("# id,nome,preco,quantidade,categoria");
            writer.newLine();

            for (int i = 1; i <= QUANTOS; i++) {
                int categoria = sorteio.nextInt(CATEGORIAS.length);
                String[] modelos = MODELOS[categoria];
                String nome = modelos[sorteio.nextInt(modelos.length)] + " "
                        + MARCAS[sorteio.nextInt(MARCAS.length)] + " "
                        + (100 + sorteio.nextInt(900));

                int minimo = FAIXA_PRECO[categoria][0];
                int maximo = FAIXA_PRECO[categoria][1];
                double preco = minimo + sorteio.nextDouble() * (maximo - minimo);
                int quantidade = sorteio.nextInt(200);

                // DEFEITO 1: produto sem descrição, o caso "dados faltantes"
                if (sorteio.nextInt(100) < 6) {
                    nome = "";
                    defeitos++;
                }
                // DEFEITO 2: preço zerado, o caso "dados inconsistentes"
                if (sorteio.nextInt(100) < 3) {
                    preco = 0;
                    defeitos++;
                }

                writer.write(String.format(java.util.Locale.US, "A%d,%s,%.2f,%d,%s",
                        i, nome, preco, quantidade, CATEGORIAS[categoria]));
                writer.newLine();
            }
        }
        return defeitos;
    }

    private static int exportarClientes() throws IOException {
        Random sorteio = new Random(43);
        int duplicados = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO_CLIENTES))) {
            writer.write("# id,nome,pedidos,valorTotal,ticketMedio,segmento");
            writer.newLine();

            String ultimoNome = "Cliente 0";
            for (int i = 1; i <= QUANTOS; i++) {
                int segmento = sorteio.nextInt(SEGMENTOS.length);

                // Quanto maior o segmento, mais pedidos e maior o ticket - com sobreposição.
                int pedidos;
                double ticket;
                if (segmento == 0) {          // Varejo
                    pedidos = 1 + sorteio.nextInt(12);
                    ticket = 50 + sorteio.nextDouble() * 1200;
                } else if (segmento == 1) {   // Atacado
                    pedidos = 5 + sorteio.nextInt(40);
                    ticket = 800 + sorteio.nextDouble() * 4000;
                } else {                      // Corporativo
                    pedidos = 2 + sorteio.nextInt(25);
                    ticket = 3000 + sorteio.nextDouble() * 20000;
                }
                double valorTotal = pedidos * ticket;
                String nome = "Cliente " + i;

                // DEFEITO: registro duplicado, o caso "informações duplicadas"
                if (i > 1 && sorteio.nextInt(100) < 4) {
                    nome = ultimoNome;
                    duplicados++;
                }
                ultimoNome = nome;

                writer.write(String.format(java.util.Locale.US, "C%d,%s,%d,%.2f,%.2f,%s",
                        i, nome, pedidos, valorTotal, ticket, SEGMENTOS[segmento]));
                writer.newLine();
            }
        }
        return duplicados;
    }

    public static String[] categorias() {
        return CATEGORIAS.clone();
    }

    public static String[] segmentos() {
        return SEGMENTOS.clone();
    }
}
