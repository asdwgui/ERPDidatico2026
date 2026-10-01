package com.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Produto {
    private String id;
    private String nome;
    private BigDecimal preco;
    private int quantidade;

    public Produto(String id, String nome, BigDecimal preco, int quantidade) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.quantidade = quantidade;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void adicionarEstoque(int qtd) {
        this.quantidade += qtd;
    }

    public boolean removerEstoque(int qtd) {
        if (qtd > this.quantidade) {
            return false;
        }
        this.quantidade -= qtd;
        return true;
    }

    @Override
    public String toString() {
        return id + "," + nome + "," + preco.toPlainString() + "," + quantidade;
    }

    public static Produto fromString(String str) {
        String[] parts = str.split(",");
        int quantidade = parts.length >= 4 ? Integer.parseInt(parts[3]) : 0;
        return new Produto(parts[0], parts[1],
                new BigDecimal(parts[2].trim()).setScale(2, RoundingMode.HALF_UP), quantidade);
    }
}
