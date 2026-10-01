package com.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Titulo {
    private String id;
    private BigDecimal quantidade;
    private boolean paga;
    private String pessoaId;
    private String tipoTitulo; // "a pagar" ou "a receber"

    public Titulo(String id, BigDecimal quantidade, boolean paga, String pessoaId, String tipoTitulo) {
        this.id = id;
        this.quantidade = quantidade;
        this.paga = paga;
        this.pessoaId = pessoaId;
        this.tipoTitulo = tipoTitulo;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public boolean isPago() {
        return paga;
    }

    public void setPaga(boolean paga) {
        this.paga = paga;
    }

    public String getPessoaId() {
        return pessoaId;
    }

    public String getTipoTitulo() {
        return tipoTitulo;
    }

    @Override
    public String toString() {
        return id + "," + quantidade.toPlainString() + "," + paga + "," + pessoaId + "," + tipoTitulo;
    }

    public static Titulo fromString(String str) {
        String[] parts = str.split(",");
        return new Titulo(parts[0], new BigDecimal(parts[1].trim()).setScale(2, RoundingMode.HALF_UP),
                Boolean.parseBoolean(parts[2]), parts[3], parts[4]);
    }
}
