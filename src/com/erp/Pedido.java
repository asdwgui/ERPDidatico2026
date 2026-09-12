package com.erp;

import java.util.ArrayList;
import java.util.List;

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

    public int quantidadeDoProduto(String produtoId) {
        for (ItemPedido item : itens) {
            if (item.getProdutoId().equals(produtoId)) {
                return item.getQuantidade();
            }
        }
        return 0;
    }

    public double getTotal() {
        double total = 0;
        for (ItemPedido item : itens) {
            total += item.getSubtotal();
        }
        return total;
    }

    @Override
    public String toString() {
        String itensTexto = "";
        for (ItemPedido item : itens) {
            if (!itensTexto.isEmpty()) {
                itensTexto += ";";
            }
            itensTexto += item.toString();
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
