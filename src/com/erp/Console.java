package com.erp;

import java.util.Locale;
import java.util.Scanner;

public class Console {
    public static int lerInt(Scanner scanner, String pergunta) {
        while (true) {
            System.out.print(pergunta);
            String texto = scanner.nextLine().trim();
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Digite um número inteiro (ex.: 10).");
            }
        }
    }

    public static double lerDouble(Scanner scanner, String pergunta) {
        while (true) {
            System.out.print(pergunta);
            String texto = scanner.nextLine().trim().replace(",", ".");
            try {
                return Double.parseDouble(texto);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Digite um valor como 1500 ou 1500,90.");
            }
        }
    }

    public static String lerTexto(Scanner scanner, String pergunta) {
        System.out.print(pergunta);
        return scanner.nextLine().trim();
    }

    public static String moeda(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %,.2f", valor);
    }

    public static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto.toUpperCase() + " ===");
    }

    public static void aviso(String texto) {
        System.out.println("  [!] " + texto);
    }

    public static void ok(String texto) {
        System.out.println("  [ok] " + texto);
    }

    public static void linha() {
        System.out.println("-".repeat(76));
    }
}