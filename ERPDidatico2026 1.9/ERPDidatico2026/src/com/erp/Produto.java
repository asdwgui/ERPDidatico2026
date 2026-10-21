package com.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Produto {
    private String id;
    private String nome;
    private BigDecimal preco;
    private int quantidade;
    private String categoria;

    public Produto(String id, String nome, BigDecimal preco, int quantidade) {
        this(id, nome, preco, quantidade, "");
    }

    public Produto(String id, String nome, BigDecimal preco, int quantidade, String categoria) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.quantidade = quantidade;
        this.categoria = (categoria == null) ? "" : categoria;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = (categoria == null) ? "" : categoria;
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
        return id + "," + nome + "," + preco.toPlainString() + "," + quantidade + "," + categoria;
    }

    public static Produto fromString(String str) {
        String[] parts = str.split(",");
        int quantidade = parts.length >= 4 ? Integer.parseInt(parts[3]) : 0;
        String categoria = parts.length >= 5 ? parts[4].trim() : "";
        return new Produto(parts[0], parts[1],
                new BigDecimal(parts[2].trim()).setScale(2, RoundingMode.HALF_UP), quantidade, categoria);
    }
}