package com.erp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Idioma {

    public static final String PADRAO = "pt";

    private static String atual = PADRAO;
    private static Map<String, String> textos = new LinkedHashMap<>();

    private static int chavesFaltando = 0;

    static {
        usar(PADRAO);
    }

    public static void usar(String codigo) {
        try {
            textos = carregarArquivo(codigo);
            atual = codigo;
            chavesFaltando = 0;
        } catch (IOException e) {
            System.out.println("[!] Arquivo de idioma " + arquivoDe(codigo) + " não encontrado. Mantendo "
                    + atual + ".");
        }
    }

    public static String atual() {
        return atual;
    }

    public static String arquivoDe(String codigo) {
        return "textos_" + codigo + ".txt";
    }

    public static Map<String, String> carregarArquivo(String codigo) throws IOException {
        Map<String, String> mapa = new LinkedHashMap<>();
        List<String> linhas = Files.readAllLines(Path.of(arquivoDe(codigo)), StandardCharsets.UTF_8);
        for (String linha : linhas) {
            String limpa = linha.trim();
            if (limpa.isEmpty() || limpa.startsWith("#")) {
                continue;
            }
            int igual = limpa.indexOf('=');
            if (igual <= 0) {
                continue;
            }
            mapa.put(limpa.substring(0, igual).trim(), limpa.substring(igual + 1).trim());
        }
        return mapa;
    }

    public static String t(String chave) {
        String texto = textos.get(chave);
        if (texto == null) {
            chavesFaltando++;
            return "[" + chave + "]";
        }
        return texto;
    }

    public static String t(String chave, Object... valores) {
        return String.format(locale(), t(chave), valores);
    }

    public static int getChavesFaltando() {
        return chavesFaltando;
    }

    public static Locale locale() {
        String tag = textos.get("formato.locale");
        if (tag == null || tag.isEmpty()) {
            tag = "pt-BR";
        }
        return Locale.forLanguageTag(tag);
    }

    public static String config(String chave, String padrao) {
        String valor = textos.get(chave);
        return (valor == null || valor.isEmpty()) ? padrao : valor;
    }
}
