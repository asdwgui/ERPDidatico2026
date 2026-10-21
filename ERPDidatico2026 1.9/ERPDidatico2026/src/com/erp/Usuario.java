package com.erp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Usuario {
    private String username;
    private String senhaHash;
    private String role; // Admin, Operador, LiderProjeto ou MembroProjeto
    private String idioma;
    private String gerenteFuncional;
    private String gerenteProjeto;

    public Usuario(String username, String senhaHash, String role) {
        this(username, senhaHash, role, Idioma.PADRAO);
    }

    public Usuario(String username, String senhaHash, String role, String idioma) {
        this(username, senhaHash, role, idioma, "", "");
    }

    public Usuario(String username, String senhaHash, String role, String idioma,
                   String gerenteFuncional, String gerenteProjeto) {
        this.username = username;
        this.senhaHash = senhaHash;
        this.role = role;
        this.idioma = idioma;
        this.gerenteFuncional = (gerenteFuncional == null) ? "" : gerenteFuncional;
        this.gerenteProjeto = (gerenteProjeto == null) ? "" : gerenteProjeto;
    }

    public String getUsername() { return username; }

    public String getSenhaHash() { return senhaHash; }

    public String getRole() { return role; }

    public String getIdioma() { return idioma; }

    public void setIdioma(String idioma) { this.idioma = idioma; }

    public void setRole(String role) { this.role = role; }

    public String getGerenteFuncional() { return gerenteFuncional; }

    public String getGerenteProjeto() { return gerenteProjeto; }

    public void setGerentes(String gerenteFuncional, String gerenteProjeto) {
        this.gerenteFuncional = (gerenteFuncional == null) ? "" : gerenteFuncional;
        this.gerenteProjeto = (gerenteProjeto == null) ? "" : gerenteProjeto;
    }

    public boolean temDuploReporte() {
        return !gerenteFuncional.isEmpty() && !gerenteProjeto.isEmpty();
    }

    @Override
    public String toString() {
        return username + "," + senhaHash + "," + role + "," + idioma + ","
                + gerenteFuncional + "," + gerenteProjeto;
    }

    public static Usuario fromString(String str) {
        String[] p = str.split(",", -1);
        String idioma = (p.length >= 4 && !p[3].isBlank()) ? p[3] : Idioma.PADRAO;
        String funcional = (p.length >= 5) ? p[4].trim() : "";
        String projeto = (p.length >= 6) ? p[5].trim() : "";
        return new Usuario(p[0], p[1], p[2], idioma, funcional, projeto);
    }

    public static String hash(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
