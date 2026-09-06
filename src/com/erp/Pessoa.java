package com.erp;

public class Pessoa {
    private String id;
    private int tipo; // 1 - Cliente, 2 - Fornecedor, 3 - Funcionario
    private String nome;
    private String baseLegal; // NOVO: LGPD

    public Pessoa(String id, int tipo, String nome, String baseLegal) {
        this.id = id;
        this.tipo = tipo;
        this.nome = nome;
        this.baseLegal = baseLegal;
    }

    public String getId() { return id; }
    public int getTipo() { return tipo; }
    public String getNome() { return nome; }
    public String getBaseLegal() { return baseLegal; }
    public void anonimizar() {
        this.nome = "ANONIMIZADO";
    }

    @Override
    public String toString() {
        return id + "," + tipo + "," + nome + "," + baseLegal;
    }

    public static Pessoa fromString(String str) {
        String[] parts = str.split(",");
        String baseLegal = parts.length >= 4 ? parts[3] : "Não informado";
        return new Pessoa(parts[0], Integer.parseInt(parts[1]), parts[2], baseLegal);
    }
}
