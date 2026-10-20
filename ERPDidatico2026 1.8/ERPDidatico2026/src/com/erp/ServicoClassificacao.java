package com.erp;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Random;

// O "endpoint de teste" que simula a chamada à API do J48.
public class ServicoClassificacao {

    private static final int LATENCIA_MINIMA = 20;
    private static final int LATENCIA_MAXIMA = 120;

    private static int falhasPorCento = 0;

    private static final int TEMPO_LIMITE_MS = 200;

    private static final Random SORTEIO = new Random(8);

    private static Classificador classificadorProdutos;
    private static ClassificadorClientes classificadorClientes;
    private static boolean tentouCarregar = false;

    public static class ServicoIndisponivelException extends Exception {
        public ServicoIndisponivelException(String mensagem) {
            super(mensagem);
        }
    }

    public static class Resposta {
        public final String sugestao;
        public final double confianca;
        public final long ms;

        public Resposta(String sugestao, double confianca, long ms) {
            this.sugestao = sugestao;
            this.confianca = confianca;
            this.ms = ms;
        }
    }

    public static void setFalhasPorCento(int porcentagem) {
        falhasPorCento = Math.max(0, Math.min(100, porcentagem));
    }

    public static int getFalhasPorCento() {
        return falhasPorCento;
    }

    private static void garantirCarregado() throws ServicoIndisponivelException {
        if (tentouCarregar) {
            if (classificadorProdutos == null) {
                throw new ServicoIndisponivelException(Idioma.t("ia.erro.modelo"));
            }
            return;
        }
        tentouCarregar = true;
        try {
            Classificador produtos = new Classificador();
            produtos.treinar(produtos.carregarAmostra(ExportadorAmostra.ARQUIVO_PRODUTOS));
            classificadorProdutos = produtos;

            ClassificadorClientes clientes = new ClassificadorClientes();
            clientes.treinar(clientes.carregarAmostra(ExportadorAmostra.ARQUIVO_CLIENTES));
            classificadorClientes = clientes;
        } catch (IOException e) {
            throw new ServicoIndisponivelException(
                    Idioma.t("ia.erro.amostra"));
        }
    }

    public static Resposta classificarProduto(String nome, BigDecimal preco)
            throws ServicoIndisponivelException {
        long inicio = System.nanoTime();
        garantirCarregado();
        simularRede();
        ArvoreJ48.Resultado resultado = classificadorProdutos.sugerir(nome, preco);
        return new Resposta(resultado.categoria, resultado.confianca,
                (System.nanoTime() - inicio) / 1_000_000);
    }

    public static Resposta classificarCliente(int pedidos, double valorTotal)
            throws ServicoIndisponivelException {
        long inicio = System.nanoTime();
        garantirCarregado();
        simularRede();
        ArvoreJ48.Resultado resultado = classificadorClientes.sugerir(pedidos, valorTotal);
        return new Resposta(resultado.categoria, resultado.confianca,
                (System.nanoTime() - inicio) / 1_000_000);
    }

    private static void simularRede() throws ServicoIndisponivelException {
        if (falhasPorCento > 0 && SORTEIO.nextInt(100) < falhasPorCento) {
            throw new ServicoIndisponivelException(Idioma.t("ia.erro.servidor"));
        }
        int latencia = LATENCIA_MINIMA + SORTEIO.nextInt(LATENCIA_MAXIMA - LATENCIA_MINIMA);
        try {
            Thread.sleep(latencia);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicoIndisponivelException(Idioma.t("ia.erro.interrompida"));
        }
        if (latencia > TEMPO_LIMITE_MS) {
            throw new ServicoIndisponivelException(Idioma.t("ia.erro.limite", TEMPO_LIMITE_MS));
        }
    }

    public static Classificador modeloProdutos() throws ServicoIndisponivelException {
        garantirCarregado();
        return classificadorProdutos;
    }

    public static ClassificadorClientes modeloClientes() throws ServicoIndisponivelException {
        garantirCarregado();
        return classificadorClientes;
    }
}