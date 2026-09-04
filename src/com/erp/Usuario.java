package com.erp;

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
        return String.valueOf(senha.hashCode());
    }
}
