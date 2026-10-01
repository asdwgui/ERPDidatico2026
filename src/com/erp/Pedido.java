package com.erp;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class Pedido {
    private String id;
    private String clienteId;
    private String data;
    private List<ItemPedido> itens;

    public Pedido(String id, String clienteId, String data) {
        this.id = id;
        this.clienteId = clienteId;
        this.data = data;
        this.itens = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getData() {
        return data;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void adicionarItem(ItemPedido novo) {
        for (ItemPedido item : itens) {
            if (item.getProdutoId().equals(novo.getProdutoId())) {
                item.adicionarQuantidade(novo.getQuantidade());
                return;
            }
        }
        itens.add(novo);
    }

    public boolean removerItem(String produtoId) {
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).getProdutoId().equals(produtoId)) {
                itens.remove(i);
                return true;
            }
        }
        return false;
    }

    public int quantidadeDoProduto(String produtoId) {
        for (ItemPedido item : itens) {
            if (item.getProdutoId().equals(produtoId)) {
                return item.getQuantidade();
            }
        }
        return 0;
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder itensTexto = new StringBuilder();
        for (ItemPedido item : itens) {
            if (itensTexto.length() > 0) {
                itensTexto.append(";");
            }
            itensTexto.append(item.toString());
        }
        return id + "," + clienteId + "," + data + "," + itensTexto;
    }

    public static Pedido fromString(String str) {
        String[] parts = str.split(",");
        Pedido pedido = new Pedido(parts[0], parts[1], parts[2]);
        for (String itemTexto : parts[3].split(";")) {
            pedido.adicionarItem(ItemPedido.fromString(itemTexto));
        }
        return pedido;
    }
}
