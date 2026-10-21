package com.erp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArvoreJ48 {

    public static class Instancia {
        public final double[] valores;
        public final String classe;

        public Instancia(double[] valores, String classe) {
            this.valores = valores;
            this.classe = classe;
        }
    }

    private static class No {
        int atributo = -1;
        double corte;
        No esquerda;  // valor <= corte
        No direita;   // valor >  corte
        String classe;        // só na folha
        double confianca;     // só na folha: proporção da classe vencedora
        int instancias;       // quantas linhas de treino caíram aqui

        boolean isFolha() {
            return atributo < 0;
        }
    }

    private final String[] nomesAtributos;
    private final int minimoPorFolha;
    private final int profundidadeMaxima;
    private No raiz;

    // evita overfitting.
    public ArvoreJ48(String[] nomesAtributos, int minimoPorFolha, int profundidadeMaxima) {
        this.nomesAtributos = nomesAtributos;
        this.minimoPorFolha = minimoPorFolha;
        this.profundidadeMaxima = profundidadeMaxima;
    }

    public void treinar(List<Instancia> treino) {
        raiz = construir(treino, 0);
    }

    private No construir(List<Instancia> dados, int profundidade) {
        No no = new No();
        no.instancias = dados.size();

        Map<String, Integer> contagem = contarClasses(dados);
        String vencedora = classeMaisComum(contagem);
        no.classe = vencedora;
        no.confianca = dados.isEmpty() ? 0 : (double) contagem.get(vencedora) / dados.size();

        if (contagem.size() <= 1 || dados.size() < minimoPorFolha * 2
                || profundidade >= profundidadeMaxima) {
            return no;
        }

        double entropiaPai = entropia(contagem, dados.size());

        List<double[]> candidatos = new ArrayList<>(); // {atributo, corte, ganho, razao}
        double somaGanhos = 0;
        int quantosComGanho = 0;

        for (int atributo = 0; atributo < nomesAtributos.length; atributo++) {
            for (double corte : cortesCandidatos(dados, atributo)) {
                List<Instancia> esquerda = new ArrayList<>();
                List<Instancia> direita = new ArrayList<>();
                for (Instancia i : dados) {
                    if (i.valores[atributo] <= corte) {
                        esquerda.add(i);
                    } else {
                        direita.add(i);
                    }
                }
                if (esquerda.size() < minimoPorFolha || direita.size() < minimoPorFolha) {
                    continue;
                }

                double n = dados.size();
                double pesoEsq = esquerda.size() / n;
                double pesoDir = direita.size() / n;
                double entropiaFilhos =
                        pesoEsq * entropia(contarClasses(esquerda), esquerda.size())
                                + pesoDir * entropia(contarClasses(direita), direita.size());

                double ganho = entropiaPai - entropiaFilhos;
                if (ganho <= 1e-9) {
                    continue;
                }

                // split info: o quanto a própria divisão é desbalanceada
                double splitInfo = -(pesoEsq * log2(pesoEsq) + pesoDir * log2(pesoDir));
                double razao = (splitInfo <= 0) ? 0 : ganho / splitInfo;

                candidatos.add(new double[] { atributo, corte, ganho, razao });
                somaGanhos += ganho;
                quantosComGanho++;
            }
        }

        if (candidatos.isEmpty()) {
            return no;
        }

        double ganhoMedio = somaGanhos / quantosComGanho;

        int melhorAtributo = -1;
        double melhorCorte = 0;
        double melhorRazao = -1;
        for (double[] c : candidatos) {
            if (c[2] < ganhoMedio - 1e-9) {
                continue;
            }
            if (c[3] > melhorRazao + 1e-9) {
                melhorRazao = c[3];
                melhorAtributo = (int) c[0];
                melhorCorte = c[1];
            }
        }

        if (melhorAtributo < 0) {
            return no;
        }

        List<Instancia> esquerda = new ArrayList<>();
        List<Instancia> direita = new ArrayList<>();
        for (Instancia i : dados) {
            if (i.valores[melhorAtributo] <= melhorCorte) {
                esquerda.add(i);
            } else {
                direita.add(i);
            }
        }

        no.atributo = melhorAtributo;
        no.corte = melhorCorte;
        no.esquerda = construir(esquerda, profundidade + 1);
        no.direita = construir(direita, profundidade + 1);
        return no;
    }

    private List<Double> cortesCandidatos(List<Instancia> dados, int atributo) {
        List<Double> valores = new ArrayList<>();
        for (Instancia i : dados) {
            valores.add(i.valores[atributo]);
        }
        valores.sort(null);

        List<Double> cortes = new ArrayList<>();
        for (int i = 1; i < valores.size(); i++) {
            double a = valores.get(i - 1);
            double b = valores.get(i);
            if (b - a > 1e-9) {
                cortes.add((a + b) / 2);
            }
        }

        if (cortes.size() > 40) {
            List<Double> reduzidos = new ArrayList<>();
            int passo = cortes.size() / 40;
            for (int i = 0; i < cortes.size(); i += passo) {
                reduzidos.add(cortes.get(i));
            }
            return reduzidos;
        }
        return cortes;
    }

    public Resultado classificar(double[] valores) {
        No no = raiz;
        if (no == null) {
            return new Resultado("", 0);
        }
        while (!no.isFolha()) {
            no = (valores[no.atributo] <= no.corte) ? no.esquerda : no.direita;
        }
        return new Resultado(no.classe, no.confianca);
    }

    public static class Resultado {
        public final String categoria;
        public final double confianca;

        public Resultado(String categoria, double confianca) {
            this.categoria = categoria;
            this.confianca = confianca;
        }
    }

    public String desenhar(int profundidadeMaxima) {
        StringBuilder texto = new StringBuilder();
        desenhar(raiz, "", 0, profundidadeMaxima, texto);
        return texto.toString();
    }

    private void desenhar(No no, String prefixo, int nivel, int limite, StringBuilder texto) {
        if (no == null) {
            return;
        }
        if (no.isFolha()) {
            texto.append(prefixo).append("-> ").append(no.classe)
                    .append(String.format(" (%d casos, %.0f%% de certeza)%n",
                            no.instancias, no.confianca * 100));
            return;
        }
        if (nivel >= limite) {
            texto.append(prefixo).append("-> ... (").append(no.instancias).append(" casos)\n");
            return;
        }
        String pergunta = nomesAtributos[no.atributo];
        texto.append(prefixo).append(pergunta).append(" <= ")
                .append(String.format("%.2f", no.corte)).append("?\n");
        desenhar(no.esquerda, prefixo + "   sim: ", nivel + 1, limite, texto);
        desenhar(no.direita, prefixo + "   nao: ", nivel + 1, limite, texto);
    }

    public int contarFolhas() {
        return contarFolhas(raiz);
    }

    private int contarFolhas(No no) {
        if (no == null) {
            return 0;
        }
        return no.isFolha() ? 1 : contarFolhas(no.esquerda) + contarFolhas(no.direita);
    }

    private static Map<String, Integer> contarClasses(List<Instancia> dados) {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (Instancia i : dados) {
            contagem.merge(i.classe, 1, Integer::sum);
        }
        return contagem;
    }

    private static String classeMaisComum(Map<String, Integer> contagem) {
        String melhor = "";
        int maior = -1;
        for (Map.Entry<String, Integer> e : contagem.entrySet()) {
            if (e.getValue() > maior) {
                maior = e.getValue();
                melhor = e.getKey();
            }
        }
        return melhor;
    }

    // Entropia: 0 quando o grupo é todo de uma classe só.
    private static double entropia(Map<String, Integer> contagem, int total) {
        if (total == 0) {
            return 0;
        }
        double soma = 0;
        for (int quantidade : contagem.values()) {
            double p = (double) quantidade / total;
            soma -= p * log2(p);
        }
        return soma;
    }

    private static double log2(double x) {
        return (x <= 0) ? 0 : Math.log(x) / Math.log(2);
    }
}
