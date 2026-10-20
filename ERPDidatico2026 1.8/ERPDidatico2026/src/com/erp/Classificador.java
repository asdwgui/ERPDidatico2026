package com.erp;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Classificador {

    private static final int PALAVRAS_NO_VOCABULARIO = 40;
    private static final int MINIMO_POR_FOLHA = 4;
    private static final int PROFUNDIDADE_MAXIMA = 30;

    private List<String> vocabulario = new ArrayList<>();
    private ArvoreJ48 arvore;
    private String classeMaisComum = "";

    // O relatório da limpeza
    public static class Limpeza {
        public int lidos;
        public int semNome;
        public int precoInvalido;
        public int aproveitados;

        public String resumo() {
            return String.format(
                    "%d registros lidos | %d sem descrição | %d com preço inválido | %d aproveitados (%.1f%%)",
                    lidos, semNome, precoInvalido, aproveitados, 100.0 * aproveitados / lidos);
        }
    }

    private Limpeza limpeza = new Limpeza();

    public Limpeza getLimpeza() {
        return limpeza;
    }

    public static class ProdutoRotulado {
        public final String nome;
        public final double preco;
        public final String categoria;

        public ProdutoRotulado(String nome, double preco, String categoria) {
            this.nome = nome;
            this.preco = preco;
            this.categoria = categoria;
        }
    }

    public List<ProdutoRotulado> carregarAmostra(String arquivo) throws IOException {
        List<ProdutoRotulado> limpos = new ArrayList<>();
        limpeza = new Limpeza();

        for (String linha : Files.readAllLines(Path.of(arquivo), StandardCharsets.UTF_8)) {
            String limpa = linha.trim();
            if (limpa.isEmpty() || limpa.startsWith("#")) {
                continue;
            }
            String[] p = limpa.split(",");
            if (p.length < 5) {
                continue;
            }
            limpeza.lidos++;

            String nome = p[1].trim();
            double preco;
            try {
                preco = Double.parseDouble(p[2].trim());
            } catch (NumberFormatException e) {
                preco = 0;
            }
            String categoria = p[4].trim();

            if (nome.isEmpty()) {
                limpeza.semNome++;
                continue;
            }
            if (preco <= 0) {
                limpeza.precoInvalido++;
                continue;
            }

            limpos.add(new ProdutoRotulado(nome, preco, categoria));
            limpeza.aproveitados++;
        }
        return limpos;
    }

    public void treinar(List<ProdutoRotulado> treino) {
        vocabulario = montarVocabulario(treino);

        String[] nomes = new String[1 + vocabulario.size()];
        nomes[0] = "preço";
        for (int i = 0; i < vocabulario.size(); i++) {
            nomes[1 + i] = "tem \"" + vocabulario.get(i) + "\" no nome";
        }

        List<ArvoreJ48.Instancia> instancias = new ArrayList<>();
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (ProdutoRotulado p : treino) {
            instancias.add(new ArvoreJ48.Instancia(atributos(p.nome, p.preco), p.categoria));
            contagem.merge(p.categoria, 1, Integer::sum);
        }

        int maior = -1;
        for (Map.Entry<String, Integer> e : contagem.entrySet()) {
            if (e.getValue() > maior) {
                maior = e.getValue();
                classeMaisComum = e.getKey();
            }
        }

        arvore = new ArvoreJ48(nomes, MINIMO_POR_FOLHA, PROFUNDIDADE_MAXIMA);
        arvore.treinar(instancias);
    }

    private List<String> montarVocabulario(List<ProdutoRotulado> treino) {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (ProdutoRotulado p : treino) {
            for (String palavra : separarPalavras(p.nome)) {
                contagem.merge(palavra, 1, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> ordenadas = new ArrayList<>(contagem.entrySet());
        ordenadas.sort((a, b) -> {
            int porContagem = Integer.compare(b.getValue(), a.getValue());
            return (porContagem != 0) ? porContagem : a.getKey().compareTo(b.getKey());
        });

        List<String> escolhidas = new ArrayList<>();
        for (Map.Entry<String, Integer> e : ordenadas) {
            if (escolhidas.size() >= PALAVRAS_NO_VOCABULARIO) {
                break;
            }
            escolhidas.add(e.getKey());
        }
        return escolhidas;
    }

    public static List<String> separarPalavras(String nome) {
        List<String> palavras = new ArrayList<>();
        if (nome == null) {
            return palavras;
        }
        for (String bruta : nome.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (bruta.length() >= 3) {
                palavras.add(bruta);
            }
        }
        return palavras;
    }

    private double[] atributos(String nome, double preco) {
        double[] valores = new double[1 + vocabulario.size()];
        valores[0] = preco;
        List<String> palavras = separarPalavras(nome);
        for (int i = 0; i < vocabulario.size(); i++) {
            valores[1 + i] = palavras.contains(vocabulario.get(i)) ? 1 : 0;
        }
        return valores;
    }

    public ArvoreJ48.Resultado sugerir(String nome, BigDecimal preco) {
        if (arvore == null) {
            return new ArvoreJ48.Resultado(classeMaisComum, 0);
        }
        return arvore.classificar(atributos(nome, preco.doubleValue()));
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

    public List<String> getVocabulario() {
        return Collections.unmodifiableList(vocabulario);
    }

    public static int dividir(List<ProdutoRotulado> todos, double proporcaoTreino) {
        Collections.shuffle(todos, new Random(2026)); // semente fixa: resultado repetível
        return (int) Math.round(todos.size() * proporcaoTreino);
    }
}
