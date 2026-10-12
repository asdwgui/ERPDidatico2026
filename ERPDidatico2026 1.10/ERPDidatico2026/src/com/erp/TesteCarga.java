package com.erp;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TesteCarga {
    private static final int TRANSACOES = 1000;
    private static final String CLIENTE_ID = "1";
    public static void main(String[] args) throws IOException {
        Estoque estoque = new Estoque();
        Pessoa cliente = estoque.buscarPessoaPorId(CLIENTE_ID);
        if (cliente == null) {
            System.out.println("Cliente " + CLIENTE_ID + " não encontrado. Cadastre um cliente antes.");
            return;
        }
        List<Produto> comEstoque = new ArrayList<>();
        for (Produto produto : estoque.getProdutos()) {
            if (produto.getQuantidade() > 0) {
                comEstoque.add(produto);
            }
        }
        if (comEstoque.isEmpty()) {
            System.out.println("Nenhum produto com estoque. Rode o GeradorMassa antes.");
            return;
        }
        System.out.println("produtos cadastrados...: " + estoque.getProdutos().size());
        System.out.println("produtos com estoque...: " + comEstoque.size());
        System.out.println("transações por rodada..: " + TRANSACOES);
        Console.linha();
        double msNormal = rodar(estoque, cliente, comEstoque, false);
        double msLote = rodar(estoque, cliente, comEstoque, true);
        Console.linha();
        System.out.printf("%-44s %10.0f ms  (%.1f ms por venda)%n",
                "Gravando a cada venda (como está hoje):", msNormal, msNormal / TRANSACOES);
        System.out.printf("%-44s %10.0f ms  (%.1f ms por venda)%n",
                "Gravando em lote no fim (otimizado):", msLote, msLote / TRANSACOES);
        if (msLote > 0) {
            System.out.printf("%-44s %10.1f x%n", "Ganho:", msNormal / msLote);
        }
    }
    private static double rodar(Estoque estoque, Pessoa cliente, List<Produto> produtos, boolean lote)
            throws IOException {
        estoque.setModoLote(lote);
        Random sorteio = new Random(3); // mesma sequência de produtos nas duas rodadas
        String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String prefixo = lote ? "CARGA-LOTE-" : "CARGA-NORMAL-";
        long inicio = System.nanoTime();
        for (int i = 0; i < TRANSACOES; i++) {
            Produto produto = sortearComEstoque(produtos, sorteio);
            if (produto == null) {
                System.out.println("Acabou o estoque dos produtos sorteados; parando em " + i + " transações.");
                break;
            }
            Pedido pedido = new Pedido(prefixo + i, cliente.getId(), data);
            pedido.adicionarItem(new ItemPedido(produto.getId(), 1, produto.getPreco()));
            estoque.fecharPedido(pedido, cliente);
        }
        if (lote) {
            estoque.salvarTudo();
        }
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        estoque.setModoLote(false);
        System.out.println((lote ? "[lote]  " : "[normal]") + " concluído em " + String.format("%.0f", ms) + " ms");
        return ms;
    }
    private static Produto sortearComEstoque(List<Produto> produtos, Random sorteio) {
        for (int tentativa = 0; tentativa < 20; tentativa++) {
            Produto produto = produtos.get(sorteio.nextInt(produtos.size()));
            if (produto.getQuantidade() > 0) {
                return produto;
            }
        }
        return null;
    }
}