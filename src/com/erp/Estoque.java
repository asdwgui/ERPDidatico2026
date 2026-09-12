package com.erp;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Estoque {
	private final List<Produto> produtos;
	private final List<Titulo> titulos;
	private final List<Pessoa> pessoas;
	private final List<Usuario> usuarios;
	private final List<Pedido> pedidos;
	private String usuarioAtual = "Desconhecido";

	public void setUsuarioAtual(String usuario) {
		this.usuarioAtual = usuario;
	}

	private static final String PRODUTOS_ARQUIVO = "produtos.txt";
	private static final String TITULOS_ARQUIVO = "titulos.txt";
	private static final String PESSOAS_ARQUIVO = "pessoas.txt";
	private static final String USUARIOS_ARQUIVO = "usuarios.txt";
	private static final String PEDIDOS_ARQUIVO = "pedidos.txt";
	private static final int ESTOQUE_MINIMO = 5;

	public Estoque() throws IOException {
		produtos = new ArrayList<>();
		titulos = new ArrayList<>();
		pessoas = new ArrayList<>();
		usuarios = new ArrayList<>();
		pedidos = new ArrayList<>();
		carregaProduto();
		carregaTitulos();
		carregaPessoas();
		carregaUsuarios();
		carregaPedidos();
	}

	public void addPessoa(Scanner scanner) throws IOException {
		System.out.print("ID da Pessoa: ");
		String id = scanner.nextLine();
		System.out.print("Nome da Pessoa: ");
		String nome = scanner.nextLine();
		System.out.print("Tipo de Pessoa (1-Cliente, 2-Fornecedor, 3-Funcionário): ");
		int tipo = scanner.nextInt();
		scanner.nextLine();
		System.out.print("Base Legal (1-Consentimento, 2-Execução de Contrato, 3-Obrigação Legal): ");
		int opcaoBase = scanner.nextInt();
		scanner.nextLine();
		String baseLegal = switch (opcaoBase) {
			case 1 -> "Consentimento";
			case 2 -> "Execução de Contrato";
			case 3 -> "Obrigação Legal";
			default -> "Não informado";
		};
		Pessoa pessoa = new Pessoa(id, tipo, nome, baseLegal);
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
		System.out.print("Quantidade inicial em estoque: ");
		int quantidade = scanner.nextInt();
		scanner.nextLine();

		Produto produto = new Produto(id, nome, preco, quantidade);
		produtos.add(produto);
		saveProdutos();
		LogAuditoria.registrar(usuarioAtual, "CADASTRO_PRODUTO", "ID=" + id + ", Nome=" + nome + ", Qtd=" +
				quantidade);
		System.out.println("Produto adicionado com sucesso.");
	}

	public void listaProdutos() {
		System.out.println("Produtos:");
		for (Produto produto : produtos) {
			System.out.println(produto.getId() + " - " + produto.getNome() + " - R$ " + produto.getPreco()
					+ " - Estoque: " + produto.getQuantidade());
		}
	}

	public void listaPessoas() {
		System.out.println("Pessoas:");
		for (Pessoa pessoa : pessoas) {
			System.out.println(pessoa.getId() + " - " + pessoa.getNome() + " - Tipo: "
					+ pessoa.getTipo() + " - Base Legal: " + pessoa.getBaseLegal());
		}
	}

	public void anonimizarPessoa(Scanner scanner) throws IOException {
		System.out.print("ID da Pessoa a anonimizar: ");
		String id = scanner.nextLine();

		Pessoa pessoa = null;
		for (Pessoa p : pessoas) {
			if (p.getId().equals(id)) {
				pessoa = p;
				break;
			}
		}

		if (pessoa != null) {
			pessoa.anonimizar();
			savePessoas();
			LogAuditoria.registrar(usuarioAtual, "ANONIMIZACAO_PESSOA", "ID=" + id);
			System.out.println("Dados da pessoa anonimizados com sucesso.");
		} else {
			System.out.println("Pessoa não encontrada.");
		}
	}

	public void compraProduto(Scanner scanner) throws IOException {
		System.out.print("ID do Produto a comprar: ");
		String produtoId = scanner.nextLine();

		Produto produto = buscarProdutoPorId(produtoId);

		if (produto != null) {
			Pessoa fornecedor = buscarPessoaPorTipo(scanner, 2);
			if (fornecedor == null) {
				System.out.println("Fornecedor não encontrado.");
				return;
			}

			System.out.print("Quantidade comprada: ");
			int qtd = scanner.nextInt();
			scanner.nextLine();
			if (qtd <= 0) {
				System.out.println("Quantidade inválida.");
				return;
			}
			produto.adicionarEstoque(qtd);
			double total = produto.getPreco() * qtd;

			Titulo titulo = new Titulo(UUID.randomUUID().toString(), total, false, fornecedor.getId(),
					"a pagar");
			titulos.add(titulo);
			saveTitulos();
			saveProdutos();
			LogAuditoria.registrar(usuarioAtual, "COMPRA", "Produto=" + produtoId + ", Qtd=" + qtd
					+ ", Fornecedor=" + fornecedor.getId());
			System.out.println("Compra registrada. Estoque atual: " + produto.getQuantidade());
			System.out.println("Título a pagar gerado: " + titulo.getId());
		} else {
			System.out.println("Produto não encontrado.");
		}
	}

	public void vendaProduto(Scanner scanner) throws IOException {
		vendaPorPedido(scanner);
	}

	private void vendaPorPedido(Scanner scanner) throws IOException {
		Pessoa cliente = buscarPessoaPorTipo(scanner, 1);
		if (cliente == null) {
			System.out.println("Cliente não encontrado.");
			return;
		}

		String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
		Pedido pedido = new Pedido("Pedido " + (pedidos.size() + 1), cliente.getId(), data);

		System.out.println();
		System.out.println("Ponto de Venda - " + pedido.getId());
		while (true) {
			System.out.print("ID do Produto (ou FIM para fechar o pedido): ");
			String produtoId = scanner.nextLine();
			if (produtoId.equalsIgnoreCase("FIM")) {
				break;
			}

			Produto produto = buscarProdutoPorId(produtoId);
			if (produto == null) {
				System.out.println("Produto não encontrado.");
				continue;
			}

			System.out.print("Quantidade: ");
			int qtd = scanner.nextInt();
			scanner.nextLine();
			if (qtd <= 0) {
				System.out.println("Quantidade inválida.");
				continue;
			}

			int jaNoPedido = pedido.quantidadeDoProduto(produtoId);
			if (qtd + jaNoPedido > produto.getQuantidade()) {
				System.out.println("Estoque insuficiente. Disponível: " + (produto.getQuantidade() - jaNoPedido));
				continue;
			}

			pedido.adicionarItem(new ItemPedido(produtoId, qtd, produto.getPreco()));
			System.out.println("Item adicionado. Total parcial: R$ " + String.format("%.2f", pedido.getTotal()));}

		if (pedido.getItens().isEmpty()) {
			System.out.println("Pedido cancelado: nenhum produto foi adicionado.");
			return;
		}

		for (ItemPedido item : pedido.getItens()) {
			Produto produto = buscarProdutoPorId(item.getProdutoId());
			produto.removerEstoque(item.getQuantidade());
			HistoricoVendas.registrarVenda(item.getProdutoId(), item.getQuantidade());
		}


		Titulo titulo = new Titulo(UUID.randomUUID().toString(), pedido.getTotal(), false, cliente.getId(), "a receber");

		titulos.add(titulo);
		pedidos.add(pedido);

		saveProdutos();
		saveTitulos();
		savePedidos();
		LogAuditoria.registrar(usuarioAtual, "VENDA_PEDIDO", "Pedido=" + pedido.getId() + ", Cliente=" + cliente.getId() + ", Itens=" + pedido.getItens().size() + ", Total=" + pedido.getTotal());

		System.out.println(pedido.getId() + " finalizado! Total: R$ " + String.format("%.2f", pedido.getTotal()));

		System.out.println("Título a receber gerado: " + titulo.getId());
	}

	public void preverDemanda(Scanner scanner) throws IOException {
		System.out.print("ID do Produto para prever demanda: ");
		String produtoId = scanner.nextLine();

		boolean produtoExiste = produtos.stream().anyMatch(p -> p.getId().equals(produtoId));
		if (!produtoExiste) {
			System.out.println("Produto não encontrado.");
			return;
		}

		List<String[]> registros = HistoricoVendas.carregarHistorico();

		Map<LocalDate, Integer> vendasPorDia = new TreeMap<>();
		for (String[] registro : registros) {
			if (registro.length < 3 || !registro[0].equals(produtoId)) {
				continue;
			}

			try {
				int quantidade = Integer.parseInt(registro[1].trim());
				LocalDate data = LocalDate.parse(registro[2].trim());
				vendasPorDia.merge(data, quantidade, Integer::sum);
			} catch (NumberFormatException | java.time.format.DateTimeParseException e) {
				System.out.println("Aviso: linha de histórico ignorada por formato inválido.");
			}
		}

		if (vendasPorDia.size() < 2) {
			System.out.println("Ainda não há histórico suficiente para prever a demanda desse produto (é preciso vendas em pelo menos 2 dias diferentes).");
			return;
		}

		LocalDate primeiroDia = vendasPorDia.keySet().iterator().next();
		List<Double> xs = new ArrayList<>();
		List<Double> ys = new ArrayList<>();
		for (Map.Entry<LocalDate, Integer> entry : vendasPorDia.entrySet()) {
			long diaIndice = ChronoUnit.DAYS.between(primeiroDia, entry.getKey());
			xs.add((double) diaIndice);
			ys.add((double) entry.getValue());
		}

		double[] coeficientes = regressaoLinear(xs, ys);
		double m = coeficientes[0];
		double b = coeficientes[1];

		long proximoDiaIndice = ChronoUnit.DAYS.between(primeiroDia, LocalDate.now()) + 1;
		double previsao = Math.max(0, m * proximoDiaIndice + b);

		String tendencia = m >= 0 ? "crescimento" : "queda";
		System.out.println("Tendência de vendas: " + tendencia + " de aproximadamente "
				+ String.format("%.2f", Math.abs(m)) + " unidades/dia.");
		System.out.println("Previsão de demanda para amanhã: " + String.format("%.1f", previsao) + " unidades.");
	}

	private double[] regressaoLinear(List<Double> xs, List<Double> ys) {
		int n = xs.size();
		double somaX = 0, somaY = 0, somaXY = 0, somaX2 = 0;

		for (int i = 0; i < n; i++) {
			somaX += xs.get(i);
			somaY += ys.get(i);
			somaXY += xs.get(i) * ys.get(i);
			somaX2 += xs.get(i) * xs.get(i);
		}

		double m = (n * somaXY - somaX * somaY) / (n * somaX2 - somaX * somaX);
		double b = (somaY - m * somaX) / n;

		return new double[]{m, b};
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

	public void relatorioInventario() {
		System.out.println();
		System.out.println("RELATÓRIO DE INVENTÁRIO:");
		double valorTotal = 0;
		int produtosEmAlerta = 0;

		for (Produto p : produtos) {
			double valorEmEstoque = p.getPreco() * p.getQuantidade();
			valorTotal += valorEmEstoque;

			String alerta = "";
			if (p.getQuantidade() <= ESTOQUE_MINIMO) {
				alerta = " (ESTOQUE BAIXO)";
				produtosEmAlerta++;
			}
			System.out.println(p.getId() + " - " + p.getNome() + " | Qtd: " + p.getQuantidade()
					+ " | Valor em estoque: R$ " + String.format("%.2f", valorEmEstoque) + alerta);

		}

		System.out.println("----------------------------------------------------------------------");
		System.out.println("Produtos cadastrados: " + produtos.size());
		System.out.println("Produtos com estoque baixo (Menor ou igual a " + ESTOQUE_MINIMO + "): " + produtosEmAlerta);
		System.out.println("Valor total do inventário: R$ " + String.format("%.2f", valorTotal));
		LogAuditoria.registrar(usuarioAtual, "RELATORIO_INVENTARIO", "Produtos=" + produtos.size());
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

	public void listarPedidos() {
		System.out.println();
		System.out.println("Pedidos:");
		if (pedidos.isEmpty()) {
			System.out.println("Nenhum pedido registrado.");
			return;
		}
		for (Pedido pedido : pedidos) {
			System.out.println(pedido.getId() + " - Data: " + pedido.getData() + " - Cliente ID: "
					+ pedido.getClienteId() + " - Total: R$ " + String.format("%.2f", pedido.getTotal()));
			for (ItemPedido item : pedido.getItens()) {
				Produto produto = buscarProdutoPorId(item.getProdutoId());
				String nome = (produto != null) ? produto.getNome() : "(produto não encontrado)";
				System.out.println("    " + item.getQuantidade() + "x " + nome + " - R$ " + item.getPrecoUnitario() + " cada - Subtotal: R$ " + String.format("%.2f", item.getSubtotal()));
			}
			System.out.println();
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

	private Produto buscarProdutoPorId(String id) {
		for (Produto p : produtos) {
			if (p.getId().equals(id)) {
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

	private void carregaPedidos() throws IOException {
		File file = new File(PEDIDOS_ARQUIVO);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.isBlank()) pedidos.add(Pedido.fromString(line));
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

	private void savePedidos() throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(PEDIDOS_ARQUIVO))) {
			for (Pedido pedido : pedidos) {
				writer.write(pedido.toString());
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
