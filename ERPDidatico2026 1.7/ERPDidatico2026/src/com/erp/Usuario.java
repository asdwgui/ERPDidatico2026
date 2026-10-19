package com.erp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Usuario {
    private String username;
    private String senhaHash;
    private String role; // "Admin" ou "Operador"
    private String idioma;

    public Usuario(String username, String senhaHash, String role) {
        this(username, senhaHash, role, Idioma.PADRAO);
    }

    public Usuario(String username, String senhaHash, String role, String idioma) {
        this.username = username;
        this.senhaHash = senhaHash;
        this.role = role;
        this.idioma = idioma;
    }

    public String getUsername() { return username; }

    public String getSenhaHash() { return senhaHash; }

    public String getRole() { return role; }

    public String getIdioma() { return idioma; }

    public void setIdioma(String idioma) { this.idioma = idioma; }

    @Override
    public String toString() {
        return username + "," + senhaHash + "," + role + "," + idioma;
    }

    public static Usuario fromString(String str) {
        String[] p = str.split(",");
        String idioma = (p.length >= 4) ? p[3] : Idioma.PADRAO;
        return new Usuario(p[0], p[1], p[2], idioma);
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
