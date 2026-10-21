package com.erp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Projeto {

    public static final String ATIVO = "ATIVO";
    public static final String ENCERRADO = "ENCERRADO";

    private final String id;
    private String nome;
    private String lider; // login do líder de projeto
    private BigDecimal orcamento;
    private String status;
    private final List<String> membros; // logins dos membros do projeto

    public Projeto(String id, String nome, String lider, BigDecimal orcamento,
                   String status, List<String> membros) {
        this.id = id;
        this.nome = nome;
        this.lider = lider;
        this.orcamento = orcamento;
        this.status = status;
        this.membros = new ArrayList<>(membros);
    }

    public String getId() { return id; }

    public String getNome() { return nome; }

    public String getLider() { return lider; }

    public void setLider(String lider) { this.lider = lider; }

    public BigDecimal getOrcamento() { return orcamento; }

    public void setOrcamento(BigDecimal orcamento) { this.orcamento = orcamento; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public boolean isAtivo() { return ATIVO.equals(status); }

    public List<String> getMembros() { return new ArrayList<>(membros); }

    public boolean adicionarMembro(String login) {
        if (login == null || login.isBlank() || membros.contains(login)) {
            return false;
        }
        membros.add(login);
        return true;
    }

    public boolean removerMembro(String login) {
        return membros.remove(login);
    }

    public boolean participa(String login) {
        return lider.equals(login) || membros.contains(login);
    }

    @Override
    public String toString() {
        return id + "," + nome + "," + lider + "," + orcamento.toPlainString()
                + "," + status + "," + String.join(";", membros);
    }

    public static Projeto fromString(String linha) {
        String[] p = linha.split(",", -1);
        List<String> membros = new ArrayList<>();
        if (p.length >= 6 && !p[5].isBlank()) {
            for (String membro : p[5].split(";")) {
                if (!membro.isBlank()) {
                    membros.add(membro.trim());
                }
            }
        }
        return new Projeto(p[0], p[1], p[2],
                new BigDecimal(p[3].trim()).setScale(2, RoundingMode.HALF_UP),
                (p.length >= 5 && !p[4].isBlank()) ? p[4].trim() : ATIVO,
                membros);
    }

    public static List<String> statusPossiveis() {
        return Arrays.asList(ATIVO, ENCERRADO);
    }
}
