package com.erp;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Estoque estoque = new Estoque();

        String usuarioLogin = Console.lerTexto(scanner, "Usuário: ");
        String senha = Console.lerTexto(scanner, "Senha: ");

        Usuario logado = estoque.autenticar(usuarioLogin, senha);
        if (logado == null) {
            System.out.println("Login inválido");
            LogAuditoria.registrar(usuarioLogin, "LOGIN_FALHOU", "Tentativa de login inválida");
            return;
        }
        String role = logado.getRole();
        System.out.println("Bem-vindo, " + logado.getUsername() + " (" + role + ")");

        estoque.setUsuarioAtual(logado.getUsername());
        LogAuditoria.registrar(logado.getUsername(), "LOGIN", "Papel=" + role);

        while (true) {
            mostrarMenu(logado.getUsername(), role);
            int choice = Console.lerInt(scanner, "Escolha uma opção: ");

            switch (choice) {
                case 1: estoque.addProduto(scanner); break;
                case 2: estoque.listaProdutos(scanner); break;
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
                        Console.aviso("Acesso negado: essa função envolve dados pessoais e é restrita ao Admin.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou listar pessoas");
                    }
                    break;
                case 11:
                    if (role.equals("Admin")) {
                        estoque.cadastrarUsuario(scanner);
                    } else {
                        Console.aviso("Acesso negado: só Admin pode cadastrar usuários.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou cadastrar usuário");
                    }
                    break;
                case 12:
                    if (role.equals("Admin")) {
                        estoque.anonimizarPessoa(scanner);
                    } else {
                        Console.aviso("Acesso negado: só Admin pode anonimizar dados.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou anonimizar pessoa");
                    }
                    break;
                case 13:
                    if (role.equals("Admin")) {
                        estoque.testeDesempenho();
                    } else {
                        Console.aviso("Acesso negado: o teste de desempenho é restrito ao Admin.");
                    }
                    break;
                case 14:
                    if (role.equals("Admin")) {
                        estoque.preverDemanda(scanner);
                    } else {
                        Console.aviso("Acesso negado: só admin pode fazer a análise preditiva.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou acessar previsão de demanda");
                    }
                    break;
                case 0:
                    System.out.println("Saindo...");
                    return;
                default:
                    Console.aviso("Opção inválida. Escolha um número do menu.");
            }
        }
    }

    private static void mostrarMenu(String usuario, String role) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("  ERP DIDÁTICO 2026            " + usuario + " (" + role + ")");
        System.out.println("============================================================");
        System.out.println("  ESTOQUE                       VENDAS");
        System.out.println("  1. Cadastrar produto          5. Ponto de venda (novo pedido)");
        System.out.println("  2. Listar / buscar produtos   6. Listar pedidos");
        System.out.println("  3. Comprar do fornecedor");
        System.out.println("  4. Relatório de inventário    FINANCEIRO");
        System.out.println("                                7. Efetuar pagamento");
        System.out.println("  CADASTROS                     8. Títulos em aberto");
        System.out.println("  9. Cadastrar pessoa");
        System.out.println(" 10. Listar pessoas [Admin]     SISTEMA");
        System.out.println(" 11. Cadastrar usuário [Admin]  13. Teste de desempenho [Admin]");
        System.out.println(" 12. Anonimizar pessoa [Admin]  14. Prever demanda [Admin]");
        System.out.println("                                 0. Sair");
        System.out.println("============================================================");
    }
}