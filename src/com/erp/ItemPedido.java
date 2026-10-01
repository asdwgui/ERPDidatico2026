package com.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ItemPedido {
    private String produtoId;
    private int quantidade;
    private BigDecimal precoUnitario;

    public ItemPedido(String produtoId, int quantidade, BigDecimal precoUnitario) {
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

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public void adicionarQuantidade(int qtd) {
        this.quantidade += qtd;
    }


    @Override
    public String toString() {
        return produtoId + ":" + quantidade + ":" + precoUnitario.toPlainString();
    }

    public static ItemPedido fromString(String str) {
        String[] p = str.split(":");
        return new ItemPedido(p[0], Integer.parseInt(p[1]),
                new BigDecimal(p[2].trim()).setScale(2, RoundingMode.HALF_UP));
    }
}
