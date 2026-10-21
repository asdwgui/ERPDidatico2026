package com.erp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ClassificadorClientes {

    private static final int MINIMO_POR_FOLHA = 5;
    private static final int PROFUNDIDADE_MAXIMA = 6;

    private static final String[] ATRIBUTOS = {
            "nº de pedidos", "valor total comprado", "ticket médio" };

    private ArvoreJ48 arvore;
    private String classeMaisComum = "";
    private int duplicadosRemovidos;
    private int lidos;

    public static class ClienteRotulado {
        public final String nome;
        public final int pedidos;
        public final double valorTotal;
        public final double ticketMedio;
        public final String segmento;

        public ClienteRotulado(String nome, int pedidos, double valorTotal,
                               double ticketMedio, String segmento) {
            this.nome = nome;
            this.pedidos = pedidos;
            this.valorTotal = valorTotal;
            this.ticketMedio = ticketMedio;
            this.segmento = segmento;
        }
    }

    public List<ClienteRotulado> carregarAmostra(String arquivo) throws IOException {
        List<ClienteRotulado> limpos = new ArrayList<>();
        Set<String> nomesVistos = new HashSet<>();
        duplicadosRemovidos = 0;
        lidos = 0;

        for (String linha : Files.readAllLines(Path.of(arquivo), StandardCharsets.UTF_8)) {
            String limpa = linha.trim();
            if (limpa.isEmpty() || limpa.startsWith("#")) {
                continue;
            }
            String[] p = limpa.split(",");
            if (p.length < 6) {
                continue;
            }
            lidos++;

            String nome = p[1].trim();
            if (!nomesVistos.add(nome.toLowerCase(Locale.ROOT))) {
                duplicadosRemovidos++;
                continue;
            }

            limpos.add(new ClienteRotulado(nome, Integer.parseInt(p[2].trim()),
                    Double.parseDouble(p[3].trim()), Double.parseDouble(p[4].trim()),
                    p[5].trim()));
        }
        return limpos;
    }

    public String resumoLimpeza() {
        return String.format("%d registros lidos | %d duplicados removidos | %d aproveitados",
                lidos, duplicadosRemovidos, lidos - duplicadosRemovidos);
    }

    public void treinar(List<ClienteRotulado> treino) {
        List<ArvoreJ48.Instancia> instancias = new ArrayList<>();
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (ClienteRotulado c : treino) {
            instancias.add(new ArvoreJ48.Instancia(
                    atributos(c.pedidos, c.valorTotal, c.ticketMedio), c.segmento));
            contagem.merge(c.segmento, 1, Integer::sum);
        }
        int maior = -1;
        for (Map.Entry<String, Integer> e : contagem.entrySet()) {
            if (e.getValue() > maior) {
                maior = e.getValue();
                classeMaisComum = e.getKey();
            }
        }
        arvore = new ArvoreJ48(ATRIBUTOS, MINIMO_POR_FOLHA, PROFUNDIDADE_MAXIMA);
        arvore.treinar(instancias);
    }

    private static double[] atributos(int pedidos, double valorTotal, double ticketMedio) {
        return new double[] { pedidos, valorTotal, ticketMedio };
    }

    public ArvoreJ48.Resultado sugerir(int pedidos, double valorTotal) {
        double ticket = (pedidos == 0) ? 0 : valorTotal / pedidos;
        if (arvore == null) {
            return new ArvoreJ48.Resultado(classeMaisComum, 0);
        }
        return arvore.classificar(atributos(pedidos, valorTotal, ticket));
    }

    public String palpiteSemAlgoritmo() {
        return classeMaisComum;
    }

    public String desenharArvore(int niveis) {
        return (arvore == null) ? "(ainda não treinada)" : arvore.desenhar(niveis);
    }

    public int contarFolhas() {
        return (arvore == null) ? 0 : arvore.contarFolhas();
    }

    public static int dividir(List<ClienteRotulado> todos, double proporcaoTreino) {
        Collections.shuffle(todos, new Random(2026));
        return (int) Math.round(todos.size() * proporcaoTreino);
    }
}
