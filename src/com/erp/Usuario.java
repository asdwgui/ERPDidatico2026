package com.erp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Usuario {
    private String username;
    private String senhaHash;
    private String role; // "Admin" ou "Operador"

    public Usuario(String username, String senhaHash, String role) {
        this.username = username;
        this.senhaHash = senhaHash;
        this.role = role;
    }

    public String getUsername() { return username; }

    public String getSenhaHash() { return senhaHash; }

    public String getRole() { return role; }

    @Override
    public String toString() {
        return username + "," + senhaHash + "," + role;
    }

    public static Usuario fromString(String str) {
        String[] p = str.split(",");
        return new Usuario(p[0], p[1], p[2]);
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
