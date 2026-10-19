package com.erp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TesteLocalizacao {

    private static final String REFERENCIA = "pt";
    private static final String[] TRADUZIDOS = { "de" };

    private static final Map<String, Integer> LIMITES = new LinkedHashMap<>();
    static {
        for (int i = 0; i <= 15; i++) {
            LIMITES.put("menu." + i, 32);
        }
        LIMITES.put("col.id", 6);
        LIMITES.put("col.produto", 32);
        LIMITES.put("col.preco", 14);
        LIMITES.put("col.estoque", 9);
        LIMITES.put("col.qtd", 8);
        LIMITES.put("col.unitario", 13);
        LIMITES.put("col.subtotal", 13);
        LIMITES.put("col.valor", 15);
        LIMITES.put("relatorio.cadastrados", 30);
        LIMITES.put("relatorio.reposicao", 30);
        LIMITES.put("relatorio.valor.total", 30);
        LIMITES.put("pedido.total", 45);
    }

    private static final List<String> IGUAIS_OK = List.of(
            "col.id", "tela.col.id", "relatorio.situacao.ok", "venda.prompt",
            "menu.sistema", "pedidos.item");

    private static int problemas = 0;

    public static void main(String[] args) throws IOException {
        Map<String, String> base = Idioma.carregarArquivo(REFERENCIA);

        Console.linha();
        System.out.println("RELATÓRIO DE QUALIDADE DA TRADUÇÃO");
        System.out.println("Referência: " + Idioma.arquivoDe(REFERENCIA) + " (" + base.size() + " textos)");
        Console.linha();

        for (String codigo : TRADUZIDOS) {
            Map<String, String> traducao = Idioma.carregarArquivo(codigo);
            System.out.println();
            System.out.println(">>> " + Idioma.arquivoDe(codigo) + " - " + traducao.size() + " textos");

            List<String> faltando = new ArrayList<>();
            List<String> naoTraduzidas = new ArrayList<>();
            List<String> marcadores = new ArrayList<>();
            List<String> estourou = new ArrayList<>();
            long somaBase = 0;
            long somaTraduzida = 0;
            String maiorCrescimento = "";
            double piorRazao = 0;

            for (Map.Entry<String, String> entrada : base.entrySet()) {
                String chave = entrada.getKey();
                String textoBase = entrada.getValue();
                String textoTraduzido = traducao.get(chave);

                if (textoTraduzido == null) {
                    faltando.add(chave);
                    continue;
                }

                if (chave.startsWith("formato.")) {
                    continue; // configuração regional, não é frase
                }

                if (textoBase.equals(textoTraduzido) && !IGUAIS_OK.contains(chave)) {
                    naoTraduzidas.add(chave + " = \"" + textoBase + "\"");
                }

                String marcadoresBase = extrairMarcadores(textoBase);
                String marcadoresTraduzidos = extrairMarcadores(textoTraduzido);
                if (!marcadoresBase.equals(marcadoresTraduzidos)) {
                    marcadores.add(chave + ": pt tem [" + marcadoresBase + "] e " + codigo
                            + " tem [" + marcadoresTraduzidos + "]");
                }

                Integer limite = LIMITES.get(chave);
                if (limite != null && textoTraduzido.length() > limite) {
                    estourou.add(chave + ": " + textoTraduzido.length() + " caracteres, cabem "
                            + limite + " -> \"" + textoTraduzido + "\"");
                }

                somaBase += textoBase.length();
                somaTraduzida += textoTraduzido.length();
                if (textoBase.length() >= 10) {
                    double razao = (double) textoTraduzido.length() / textoBase.length();
                    if (razao > piorRazao) {
                        piorRazao = razao;
                        maiorCrescimento = chave;
                    }
                }
            }

            List<String> sobrando = new ArrayList<>();
            for (String chave : traducao.keySet()) {
                if (!base.containsKey(chave)) {
                    sobrando.add(chave);
                }
            }

            listar("Textos SEM tradução", faltando);
            listar("Chaves que não existem mais em português", sobrando);
            listar("Textos idênticos ao português (provavelmente esquecidos)", naoTraduzidas);
            listar("Marcadores %s/%d que não batem (QUEBRA O PROGRAMA)", marcadores);
            listar("Textos que não cabem no espaço da tela", estourou);

            double crescimento = 100.0 * (somaTraduzida - somaBase) / somaBase;
            System.out.printf("    Tamanho total do texto: %+.1f%% em relação ao português%n", crescimento);
            System.out.printf("    Frase que mais cresceu: %s (%.1fx)%n", maiorCrescimento, piorRazao);
        }

        Console.linha();
        if (problemas == 0) {
            System.out.println("RESULTADO: nenhum problema encontrado.");
        } else {
            System.out.println("RESULTADO: " + problemas + " ponto(s) a corrigir antes de liberar a versão.");
        }
        Console.linha();
    }

    private static void listar(String titulo, List<String> itens) {
        if (itens.isEmpty()) {
            System.out.println("  [ok] " + titulo + ": nenhum");
            return;
        }
        problemas += itens.size();
        System.out.println("  [!] " + titulo + ": " + itens.size());
        for (String item : itens) {
            System.out.println("        - " + item);
        }
    }

    private static String extrairMarcadores(String texto) {
        StringBuilder encontrados = new StringBuilder();
        for (int i = 0; i < texto.length() - 1; i++) {
            if (texto.charAt(i) == '%') {
                char tipo = texto.charAt(i + 1);
                if (tipo == '%') {
                    i++;
                } else if (tipo == 's' || tipo == 'd' || tipo == 'f') {
                    if (encontrados.length() > 0) {
                        encontrados.append(' ');
                    }
                    encontrados.append('%').append(tipo);
                }
            }
        }
        return encontrados.toString();
    }
}
