package com.erp;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Estoque estoque = new Estoque();

        System.out.print("Usuário: ");
        String usuarioLogin = scanner.nextLine();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();

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
            System.out.println("\nMenu:");
            System.out.println("1. Adicionar Produto");
            System.out.println("2. Listar Produtos");
            System.out.println("3. Comprar Produto (Fornecedor)");
            System.out.println("4. Ponto de Venda");
            System.out.println("5. Listar Pedidos");
            System.out.println("6. Relatório de Inventário");
            System.out.println("7. Efetuar Pagamento");
            System.out.println("8. Listar Títulos em Aberto");
            System.out.println("9. Cadastrar Pessoa");
            System.out.println("10. Prever Demanda de Produto [Acesso Restrito]");
            System.out.println("11. Listar Pessoas [Acesso Restrito]");
            System.out.println("12. Cadastrar Usuário [Acesso Restrito]");
            System.out.println("13. Anonimizar Dados de Pessoa [Acesso Restrito]");
            System.out.println("14. Sair");
            System.out.print("Escolha uma opção: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    estoque.addProduto(scanner);
                    break;
                case 2:
                    estoque.listaProdutos();
                    break;
                case 3:
                    estoque.compraProduto(scanner);
                    break;
                case 4:
                    estoque.vendaProduto(scanner);
                    break;
                case 5:
                    estoque.listarPedidos();
                    break;
                case 6:
                    estoque.relatorioInventario();
                    break;
                case 7:
                    estoque.fazPagamento(scanner);
                    break;
                case 8:
                    estoque.listarTitulosDeDestaque();
                    break;
                case 9:
                    estoque.addPessoa(scanner);
                    break;
                case 10:
                    if (role.equals("Admin")) {
                        estoque.preverDemanda(scanner);
                    } else {
                        System.out.println("Acesso negado: só admin pode fazer a análise preditiva.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou acessar previsão de demanda");
                    }
                    break;
                case 11:
                    if (role.equals("Admin")) {
                        estoque.listaPessoas();
                    } else {
                        System.out.println("Acesso negado: essa função envolve dados pessoais e é restrita ao Admin.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou listar pessoas");
                    }
                    break;
                case 12:
                    if (role.equals("Admin")) {
                        estoque.cadastrarUsuario(scanner);
                    } else {
                        System.out.println("Acesso negado: só Admin pode cadastrar usuários.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou cadastrar usuário");
                    }
                    break;
                case 13:
                    if (role.equals("Admin")) {
                        estoque.anonimizarPessoa(scanner);
                    } else {
                        System.out.println("Acesso negado: só Admin pode anonimizar dados.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou anonimizar pessoa");
                    }
                    break;
                case 14:
                    System.out.println("Saindo...");
                    return;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
    }
}
