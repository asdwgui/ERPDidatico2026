package com.erp;

import java.io.*;
import java.util.*;

public class Estoque {
	private List<Produto> produtos;
	private List<Titulo> titulos;
	private List<Pessoa> pessoas;
	private List<Usuario> usuarios;
	private String usuarioAtual = "Desconhecido";

	public void setUsuarioAtual(String usuario) {
		this.usuarioAtual = usuario;
	}

	private static final String PRODUTOS_ARQUIVO = "produtos.txt";
	private static final String TITULOS_ARQUIVO = "titulos.txt";
	private static final String PESSOAS_ARQUIVO = "pessoas.txt";
	private static final String USUARIOS_ARQUIVO = "usuarios.txt";

	public Estoque() throws IOException {
		produtos = new ArrayList<>();
		titulos = new ArrayList<>();
		pessoas = new ArrayList<>();
		usuarios = new ArrayList<>();
		carregaProduto();
		carregaTitulos();
		carregaPessoas();
		carregaUsuarios();
	}

	public void addPessoa(Scanner scanner) throws IOException {
		System.out.print("ID da Pessoa: ");
		String id = scanner.nextLine();
		System.out.print("Nome da Pessoa: ");
		String nome = scanner.nextLine();
		System.out.print("Tipo de Pessoa (1-Cliente, 2-Fornecedor, 3-Funcionário): ");
		int tipo = scanner.nextInt();
		scanner.nextLine();

		Pessoa pessoa = new Pessoa(id, tipo, nome);
		pessoas.add(pessoa);
		savePessoas();
		LogAuditoria.registrar(usuarioAtual, "CADASTRO_PESSOA", "ID=" + id + ", Nome=" + nome);
		System.out.println("Pessoa adicionada com sucesso.");
	}

	public void addProduto(Scanner scanner) throws IOException {
		System.out.print("ID do Produto: ");
		String id = scanner.nextLine();
		System.out.print("Nome do Produto: ");
		String nome = scanner.nextLine();
		System.out.print("Preço do Produto: ");
		double preco = scanner.nextDouble();
		scanner.nextLine();

		Produto produto = new Produto(id, nome, preco);
		produtos.add(produto);
		saveProdutos();
		LogAuditoria.registrar(usuarioAtual, "CADASTRO_PRODUTO", "ID=" + id + ", Nome=" + nome);
		System.out.println("Produto adicionado com sucesso.");
	}

	public void listaProdutos() {
		System.out.println("Produtos:");
		for (Produto produto : produtos) {
			System.out.println(produto.getId() + " - " + produto.getNome() + " - R$ " + produto.getPreco());
		}
	}

	public void listaPessoas() {
		System.out.println("Pessoas:");
		for (Pessoa pessoa : pessoas) {
			System.out.println(pessoa.getId() + " - " + pessoa.getNome() + " - Tipo: " + pessoa.getTipo());
		}
	}

	public void compraProduto(Scanner scanner) throws IOException {
		System.out.print("ID do Produto a comprar: ");
		String produtoId = scanner.nextLine();

		Produto produto = null;
		for (Produto p : produtos) {
			if (p.getId().equals(produtoId)) {
				produto = p;
				break;
			}
		}

		if (produto != null) {
			Pessoa fornecedor = buscarPessoaPorTipo(scanner, 2);
			if (fornecedor == null) {
				System.out.println("Fornecedor não encontrado.");
				return;
			}

			Titulo titulo = new Titulo(UUID.randomUUID().toString(), produto.getPreco(), false, fornecedor.getId(),
					"a pagar");
			titulos.add(titulo);
			saveTitulos();
			LogAuditoria.registrar(usuarioAtual, "COMPRA", "Produto=" + produtoId + ", Fornecedor=" + fornecedor.getId());
			System.out.println("Compra registrada. Título a pagar gerado: " + titulo.getId());
		} else {
			System.out.println("Produto não encontrado.");
		}
	}

	public void vendaProduto(Scanner scanner) throws IOException {
		System.out.print("ID do Produto a vender: ");
		String produtoId = scanner.nextLine();

		Produto produto = null;
		for (Produto p : produtos) {
			if (p.getId().equals(produtoId)) {
				produto = p;
				break;
			}
		}

		if (produto != null) {
			Pessoa cliente = buscarPessoaPorTipo(scanner, 1);
			if (cliente == null) {
				System.out.println("Cliente não encontrado.");
				return;
			}

			Titulo titulo = new Titulo(UUID.randomUUID().toString(), produto.getPreco(), false, cliente.getId(),
					"a receber");
			titulos.add(titulo);
			saveTitulos();
			LogAuditoria.registrar(usuarioAtual, "VENDA", "Produto=" + produtoId + ", Cliente=" + cliente.getId());
			System.out.println("Venda registrada. Título a receber gerado: " + titulo.getId());
		} else {
			System.out.println("Produto não encontrado.");
		}
	}

	public void fazPagamento(Scanner scanner) throws IOException {
		System.out.print("ID do Título a pagar: ");
		String tituloId = scanner.nextLine();

		Titulo titulo = null;
		for (Titulo t : titulos) {
			if (t.getId().equals(tituloId)) {
				titulo = t;
				break;
			}
		}

		if (titulo != null) {
			titulo.setPaga(true);
			saveTitulos();
			LogAuditoria.registrar(usuarioAtual, "PAGAMENTO", "Titulo=" + tituloId);
			System.out.println("Título pago com sucesso.");
		} else {
			System.out.println("Título não encontrado.");
		}
	}

	public void listarTitulosDeDestaque() {
		System.out.println("Títulos em Aberto:");
		for (Titulo title : titulos) {
			if (!title.isPago()) {
				System.out.println(title.getId() + " - R$ " + title.getQuantidade() + " - Pessoa: "
						+ title.getPessoaId() + " - Tipo: " + title.getTipoTitulo());
			}
		}
	}

	private Pessoa buscarPessoaPorTipo(Scanner scanner, int tipo) {
		System.out.print("ID da Pessoa (tipo " + tipo + "): ");
		String id = scanner.nextLine();
		for (Pessoa p : pessoas) {
			if (p.getId().equals(id) && p.getTipo() == tipo) {
				return p;
			}
		}
		return null;
	}

	private void carregaProduto() throws IOException {
		File file = new File(PRODUTOS_ARQUIVO);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.isBlank()) produtos.add(Produto.fromString(line));
				}
			}
		}
	}

	private void carregaTitulos() throws IOException {
		File file = new File(TITULOS_ARQUIVO);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.isBlank()) titulos.add(Titulo.fromString(line));
				}
			}
		}
	}

	private void carregaPessoas() throws IOException {
		File file = new File(PESSOAS_ARQUIVO);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.isBlank()) pessoas.add(Pessoa.fromString(line));
				}
			}
		}
	}

	private void carregaUsuarios() throws IOException {
		File file = new File(USUARIOS_ARQUIVO);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.isBlank()) usuarios.add(Usuario.fromString(line));
				}
			}
		} else {
			Usuario admin = new Usuario("admin", Usuario.hash("admin123"), "Admin");
			Usuario operador = new Usuario("operador", Usuario.hash("operador123"), "Operador");
			usuarios.add(admin);
			usuarios.add(operador);
			saveUsuarios();
			System.out.println("Usuários criados -> admin/admin123 (Admin) e operador/operador123 (Operador)");
		}
	}

	public Usuario autenticar(String username, String senha) {
		String hashDigitado = Usuario.hash(senha);
		for (Usuario u : usuarios) {
			if (u.getUsername().equals(username) && u.getSenhaHash().equals(hashDigitado)) {
				return u;
			}
		}
		return null;
	}

	public void cadastrarUsuario(Scanner scanner) throws IOException {
		System.out.print("Novo login: ");
		String username = scanner.nextLine();

		for (Usuario u : usuarios) {
			if (u.getUsername().equals(username)) {
				System.out.println("Já existe um usuário com esse login.");
				return;
			}
		}

		System.out.print("Senha: ");
		String senha = scanner.nextLine();
		System.out.print("Papel (1-Admin, 2-Operador): ");
		int papel = scanner.nextInt();
		scanner.nextLine();
		String role = (papel == 1) ? "Admin" : "Operador";

		Usuario novo = new Usuario(username, Usuario.hash(senha), role);
		usuarios.add(novo);
		saveUsuarios();
		System.out.println("Usuário '" + username + "' cadastrado como " + role + ".");
	}

	private void saveProdutos() throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(PRODUTOS_ARQUIVO))) {
			for (Produto product : produtos) {
				writer.write(product.toString());
				writer.newLine();
			}
		}
	}

	private void saveTitulos() throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(TITULOS_ARQUIVO))) {
			for (Titulo title : titulos) {
				writer.write(title.toString());
				writer.newLine();
			}
		}
	}

	private void savePessoas() throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(PESSOAS_ARQUIVO))) {
			for (Pessoa pessoa : pessoas) {
				writer.write(pessoa.toString());
				writer.newLine();
			}
		}
	}

	private void saveUsuarios() throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(USUARIOS_ARQUIVO))) {
			for (Usuario u : usuarios) {
				writer.write(u.toString());
				writer.newLine();
			}
		}
	}
}