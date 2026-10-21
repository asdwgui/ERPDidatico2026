package com.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Aprovacao {

    public static final String PENDENTE = "PENDENTE";
    public static final String APROVADA = "APROVADA";
    public static final String REJEITADA = "REJEITADA";

    public static final String TIPO_ORCAMENTO = "ORCAMENTO";
    public static final String TIPO_COMPRA = "COMPRA";

    private final String id;
    private final String projetoId;
    private final String tipo;
    private final BigDecimal valor;
    private final String solicitante;
    private String status;
    private final String data;
    private String decisor;
    private final String descricao;

    public Aprovacao(String id, String projetoId, String tipo, BigDecimal valor,
                     String solicitante, String status, String data, String decisor, String descricao) {
        this.id = id;
        this.projetoId = projetoId;
        this.tipo = tipo;
        this.valor = valor;
        this.solicitante = solicitante;
        this.status = status;
        this.data = data;
        this.decisor = decisor;
        this.descricao = descricao;
    }

    public String getId() { return id; }

    public String getProjetoId() { return projetoId; }

    public String getTipo() { return tipo; }

    public BigDecimal getValor() { return valor; }

    public String getSolicitante() { return solicitante; }

    public String getStatus() { return status; }

    public String getData() { return data; }

    public String getDecisor() { return decisor; }

    public String getDescricao() { return descricao; }

    public boolean isPendente() { return PENDENTE.equals(status); }

    public boolean isAprovada() { return APROVADA.equals(status); }

    public void decidir(String novoStatus, String decisor) {
        this.status = novoStatus;
        this.decisor = decisor;
    }

    @Override
    public String toString() {
        return id + "," + projetoId + "," + tipo + "," + valor.toPlainString() + ","
                + solicitante + "," + status + "," + data + "," + decisor + "," + descricao;
    }

    public static Aprovacao fromString(String linha) {
        String[] p = linha.split(",", -1);
        return new Aprovacao(p[0], p[1], p[2],
                new BigDecimal(p[3].trim()).setScale(2, RoundingMode.HALF_UP),
                p[4], p[5], p[6],
                p.length >= 8 ? p[7] : "",
                p.length >= 9 ? p[8] : "");
    }
}
