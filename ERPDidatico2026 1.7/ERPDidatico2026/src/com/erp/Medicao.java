package com.erp;

public class Medicao {
    private final String nome;
    private final long inicioNano;
    private Medicao(String nome) {
        this.nome = nome;
        this.inicioNano = System.nanoTime();
    }
    public static Medicao iniciar(String nome) {
        return new Medicao(nome);
    }
    // Mostra o tempo no console e devolve o valor em milissegundos.
    public double encerrar() {
        double ms = (System.nanoTime() - inicioNano) / 1_000_000.0;
        System.out.printf("[tempo] %-42s %9.1f ms%n", nome, ms);
        return ms;
    }
}