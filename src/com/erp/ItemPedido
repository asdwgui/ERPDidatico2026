package com.erp;

public class ItemPedido {
    private String produtoId;
    private int quantidade;
    private double precoUnitario;

    public ItemPedido(String produtoId, int quantidade, double precoUnitario) {
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public String getProdutoId() {
        return produtoId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public double getPrecoUnitario() {
        return precoUnitario;
    }

    public double getSubtotal() {
        return quantidade * precoUnitario;
    }

    public void adicionarQuantidade(int qtd) {
        this.quantidade += qtd;
    }


    @Override
    public String toString() {
        return produtoId + ":" + quantidade + ":" + precoUnitario;
    }

    public static ItemPedido fromString(String str) {
        String[] p = str.split(":");
        return new ItemPedido(p[0], Integer.parseInt(p[1]), Double.parseDouble(p[2]));
    }
}
