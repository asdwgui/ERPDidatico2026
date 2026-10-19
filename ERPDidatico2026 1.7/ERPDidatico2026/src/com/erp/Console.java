package com.erp;

import java.util.Scanner;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Console {

    public static int lerInt(Scanner scanner, String pergunta) {
        while (true) {
            System.out.print(pergunta + " ");
            String texto = scanner.nextLine().trim();
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                aviso(Idioma.t("console.int.invalido"));
            }
        }
    }

    public static BigDecimal lerDecimal(Scanner scanner, String pergunta) {
        while (true) {
            System.out.print(pergunta + " ");
            String texto = scanner.nextLine().trim().replace(",", ".");
            try {
                BigDecimal digitado = new BigDecimal(texto).setScale(2, RoundingMode.HALF_UP);
                return Formato.paraArmazenar(digitado);
            } catch (NumberFormatException e) {
                aviso(Idioma.t("console.decimal.invalido"));
            }
        }
    }

    public static String lerTexto(Scanner scanner, String pergunta) {
        System.out.print(pergunta + " ");
        return scanner.nextLine().trim();
    }

    public static String moeda(BigDecimal valor) {
        return Formato.moeda(valor);
    }

    public static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto.toUpperCase(Idioma.locale()) + " ===");
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
