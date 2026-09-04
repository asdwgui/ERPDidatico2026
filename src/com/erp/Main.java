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
            LogAuditoria.registrar(usuarioLogin, "LOGIN_FALHOU", "Tentativa de login inválida");  // NOVA LINHA
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
            System.out.println("4. Vender Produto (Cliente)");
            System.out.println("5. Efetuar Pagamento");
            System.out.println("6. Listar Títulos em Aberto");
            System.out.println("7. Cadastrar Pessoa");
            System.out.println("8. Listar Pessoas [Acesso Restrito]");
            System.out.println("9. Cadastrar Usuário [Acesso Restrito]");
            System.out.println("10. Sair");
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
                    estoque.fazPagamento(scanner);
                    break;
                case 6:
                    estoque.listarTitulosDeDestaque();
                    break;
                case 7:
                    estoque.addPessoa(scanner);
                    break;
                case 8:
                    if (role.equals("Admin")) {
                        estoque.listaPessoas();
                    } else {
                        System.out.println("Acesso negado: essa função envolve dados pessoais e é restrita ao Admin.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou listar pessoas");
                    }
                    break;
                case 9:
                    if (role.equals("Admin")) {
                        estoque.cadastrarUsuario(scanner);
                    } else {
                        System.out.println("Acesso negado: só Admin pode cadastrar usuários.");
                        LogAuditoria.registrar(logado.getUsername(), "ACESSO_NEGADO", "Tentou cadastrar usuário");
                    }
                    break;
                case 10:
                    System.out.println("Saindo...");
                    return;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
    }
}
