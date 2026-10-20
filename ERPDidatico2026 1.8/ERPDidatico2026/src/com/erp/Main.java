package com.erp;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        Medicao abertura = Medicao.iniciar(Idioma.t("menu.abertura"));

        Estoque estoque = new Estoque();
        abertura.encerrar();

        System.out.println();
        String usuarioLogin = Console.lerTexto(scanner, Idioma.t("login.usuario"));
        String senha = Console.lerTexto(scanner, Idioma.t("login.senha"));

        Usuario logado = estoque.autenticar(usuarioLogin, senha);
        if (logado == null) {
            System.out.println(Idioma.t("login.invalido"));
            LogAuditoria.registrar(usuarioLogin, "LOGIN_FALHOU", "Tentativa de login inválida");
            return;
        }

        Idioma.usar(logado.getIdioma());

        String role = logado.getRole();
        System.out.println(Idioma.t("login.bemvindo", logado.getUsername(), role));

        estoque.setUsuarioAtual(logado.getUsername());
        LogAuditoria.registrar(logado.getUsername(), "LOGIN", "Papel=" + role + ", Idioma=" + Idioma.atual());

        while (true) {
            mostrarMenu(logado.getUsername(), role);
            int choice = Console.lerInt(scanner, Idioma.t("menu.escolha"));

            switch (choice) {
                case 1: estoque.addProduto(scanner); break;
                case 2: abrirTelaConsulta(estoque); break;
                case 3: estoque.compraProduto(scanner); break;
                case 4: estoque.relatorioInventario(); break;
                case 5: estoque.vendaProduto(scanner); break;
                case 6: estoque.listarPedidos(scanner); break;
                case 7: estoque.fazPagamento(scanner); break;
                case 8: estoque.listarTitulosDeDestaque(); break;
                case 9: estoque.addPessoa(scanner); break;
                case 10:
                    if (role.equals("Admin")) {
                        estoque.listaPessoas();
                    } else {
                        Console.aviso(Idioma.t("acesso.pessoas"));
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou listar pessoas");
                    }
                    break;
                case 11:
                    if (role.equals("Admin")) {
                        estoque.cadastrarUsuario(scanner);
                    } else {
                        Console.aviso(Idioma.t("acesso.usuarios"));
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou cadastrar usuário");
                    }
                    break;
                case 12:
                    if (role.equals("Admin")) {
                        estoque.anonimizarPessoa(scanner);
                    } else {
                        Console.aviso(Idioma.t("acesso.anonimizar"));
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou anonimizar pessoa");
                    }
                    break;
                case 13:
                    if (role.equals("Admin")) {
                        estoque.testeDesempenho();
                    } else {
                        Console.aviso(Idioma.t("acesso.desempenho"));
                    }
                    break;
                case 14:
                    if (role.equals("Admin")) {
                        estoque.preverDemanda(scanner);
                    } else {
                        Console.aviso(Idioma.t("acesso.previsao"));
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou acessar previsão de demanda");
                    }
                    break;
                case 15:
                    estoque.trocarIdioma(scanner, logado);
                    break;
                case 16:
                    abrirCadastroInteligente(estoque);
                    break;
                case 17:
                    if (role.equals("Admin")) {
                        MetricasClassificacao.relatorio();
                    } else {
                        Console.aviso(Idioma.t("acesso.ia"));
                    }
                    break;
                case 0:
                    System.out.println(Idioma.t("menu.saindo"));
                    System.exit(0);
                default:
                    Console.aviso(Idioma.t("menu.invalida"));
            }
        }
    }

    private static void abrirTelaConsulta(Estoque estoque) {
        Medicao medicao = Medicao.iniciar(Idioma.t("tela.montar"));
        SwingUtilities.invokeLater(() -> new TelaConsultaProdutos(estoque.getProdutos()).setVisible(true));
        medicao.encerrar();
        System.out.println(Idioma.t("tela.aberta"));
    }

    private static final int COLUNA = 36;

    private static void abrirCadastroInteligente(Estoque estoque) {
        SwingUtilities.invokeLater(() -> new TelaCadastroInteligente(estoque).setVisible(true));
        System.out.println(Idioma.t("tela.aberta"));
    }

    private static void mostrarMenu(String usuario, String role) {
        System.out.println();
        Console.linha();
        System.out.println("  " + preencher(Idioma.t("menu.sistema"), COLUNA) + usuario + " (" + role + ")");
        Console.linha();
        linhaDupla(Idioma.t("menu.grupo.estoque"), Idioma.t("menu.grupo.vendas"));
        linhaDupla(item(1), item(5));
        linhaDupla(item(2), item(6));
        linhaDupla(item(3), "");
        linhaDupla(item(4), Idioma.t("menu.grupo.financeiro"));
        linhaDupla("", item(7));
        linhaDupla(Idioma.t("menu.grupo.cadastros"), item(8));
        linhaDupla(item(9), "");
        linhaDupla(item(10), Idioma.t("menu.grupo.sistema"));
        linhaDupla(item(11), item(13));
        linhaDupla(item(12), item(14));
        linhaDupla((""), item(15));
        linhaDupla(Idioma.t("menu.grupo.ia"), item(17));
        linhaDupla(item(16), item(0));
        Console.linha();
    }

    private static String item(int numero) {
        return String.format("%2d. %s", numero, Idioma.t("menu." + numero));
    }

    private static void linhaDupla(String esquerda, String direita) {
        System.out.println(("  " + preencher(esquerda, COLUNA) + direita).stripTrailing());
    }

    private static String preencher(String texto, int largura) {
        if (texto.length() >= largura) {
            return texto + " ";
        }
        return texto + " ".repeat(largura - texto.length());
    }
}
