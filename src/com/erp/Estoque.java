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
		String id = Console.lerTexto(scanner, "ID da Pessoa: ");

		if (buscarPessoaPorId(id) != null) {
			System.out.println("Já existe uma pessoa com o ID " + id + ". Cadastro cancelado.");
			return;
		}

		String nome = Console.lerTexto(scanner, "Nome da Pessoa: ");

		int tipo;
		while (true) {
			tipo = Console.lerInt(scanner, "Tipo de Pessoa (1-Cliente, 2-Fornecedor, 3-Funcionário): ");
			if (tipo >= 1 && tipo <= 3) {
				break;
			}
			System.out.println("Opção inválida. Digite 1, 2 ou 3.");
		}

		int opcaoBase;
		while (true) {
			opcaoBase = Console.lerInt(scanner, "Base Legal (1-Consentimento, 2-Execução de Contrato, 3-Obrigação Legal): ");
			if (opcaoBase >= 1 && opcaoBase <= 3) {
				break;
			}
			System.out.println("Opção inválida. Digite 1, 2 ou 3.");
		}

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
		Console.titulo("Cadastro de produto");
		String id = Console.lerTexto(scanner, "ID do Produto: ");
		if (id.isEmpty()) {
			Console.aviso("O ID não pode ficar em branco.");
			return;
		}
		if (buscarProdutoPorId(id) != null) {
			Console.aviso("Já existe um produto com o ID " + id + ". Use outro ID.");
			return;
		}
		String nome = Console.lerTexto(scanner, "Nome do Produto: ");
		if (nome.isEmpty() || nome.contains(",")) {
			Console.aviso("O nome não pode ficar em branco nem conter vírgula.");
			return;
		}
		double preco = Console.lerDouble(scanner, "Preço do Produto (ex.: 1500,90): ");
		if (preco <= 0) {
			Console.aviso("O preço precisa ser maior que zero.");
			return;
		}
		int quantidade = Console.lerInt(scanner, "Quantidade inicial em estoque: ");
		if (quantidade < 0) {
			Console.aviso("A quantidade não pode ser negativa.");
			return;
		}

		Produto produto = new Produto(id, nome, preco, quantidade);
		registrarProduto(produto);
		saveProdutos();
		LogAuditoria.registrar(usuarioAtual, "CADASTRO_PRODUTO", "ID=" + id + ", Nome=" + nome + ", Qtd=" + quantidade);
		Console.ok("Produto cadastrado: " + nome + " - " + Console.moeda(preco) + " - estoque " + quantidade);
	}

	public void listaProdutos(Scanner scanner) {
		String filtro = Console.lerTexto(scanner, "Filtrar por nome (Enter para ver todos): ").toLowerCase();
		Console.titulo("Produtos");
		if (produtos.isEmpty()) {
			System.out.println("Nenhum produto cadastrado ainda. Use a opção 1 do menu.");
			return;
		}
		System.out.printf("%-6s %-32s %14s %9s%n", "ID", "PRODUTO", "PREÇO", "ESTOQUE");
		Console.linha();
		int mostrados = 0;
		for (Produto produto : produtos) {
			if (!filtro.isEmpty() && !produto.getNome().toLowerCase().contains(filtro)) {
				continue;
			}
			System.out.printf("%-6s %-32s %14s %9d%n", produto.getId(), cortar(produto.getNome(), 32),
					Console.moeda(produto.getPreco()), produto.getQuantidade());
			mostrados++;
		}
		Console.linha();
		if (mostrados == 0) {
			System.out.println("Nenhum produto com \"" + filtro + "\" no nome.");
		} else {
			System.out.println(mostrados + " produto(s) exibido(s) de " + produtos.size() + " cadastrado(s).");
		}
	}
	public void listaPessoas() {
		System.out.println("Pessoas:");

		if (pessoas.isEmpty()) {
			System.out.println("Nenhuma pessoa cadastrada.");
			return;
		}

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

	private double arredondar(double valor) {
		return Math.round(valor * 100) / 100.0;
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

			System.out.print("Quantidade comprada (Numeral): ");
			int qtd = scanner.nextInt();
			scanner.nextLine();
			if (qtd <= 0) {
				System.out.println("Quantidade inválida.");
				return;
			}

			System.out.print("Custo unitário pago ao fornecedor: ");
			double custoUnitario = scanner.nextDouble();
			scanner.nextLine();
			if (custoUnitario <= 0) {
				System.out.println("Custo inválido.");
				return;
			}

			produto.adicionarEstoque(qtd);
			double total = arredondar(custoUnitario * qtd);

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
		Console.titulo("Ponto de venda");
		Pessoa cliente = buscarPessoaPorTipo(scanner, 1);
		if (cliente == null) {
			Console.aviso("Cliente não encontrado.");
			return;
		}

		String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
		Pedido pedido = new Pedido("Pedido " + (pedidos.size() + 1), cliente.getId(), data);

		System.out.println(pedido.getId() + " | cliente ID " + cliente.getId() + " | " + data);
		System.out.println("Digite o ID do produto, R para remover um item ou FIM para fechar o pedido.");
		while (true) {
			String produtoId = Console.lerTexto(scanner, "> ");
			if (produtoId.equalsIgnoreCase("FIM")) {
				break;
			}
			if (produtoId.equalsIgnoreCase("R")) {
				String idRemover = Console.lerTexto(scanner, "ID do produto a remover do pedido: ");
				if (pedido.removerItem(idRemover)) {
					Console.ok("Item removido. Total parcial: " + Console.moeda(pedido.getTotal()));
				} else {
					Console.aviso("Esse produto não está no pedido.");
				}
				continue;
			}

			Produto produto = buscarProdutoPorId(produtoId);
			if (produto == null) {
				Console.aviso("Produto não encontrado. Confira o ID na opção 2 do menu.");
				continue;
			}

			int qtd = Console.lerInt(scanner, "Quantidade de " + produto.getNome() + ": ");
			if (qtd <= 0) {
				Console.aviso("Quantidade inválida.");
				continue;
			}

			int jaNoPedido = pedido.quantidadeDoProduto(produtoId);
			if (qtd + jaNoPedido > produto.getQuantidade()) {
				Console.aviso("Estoque insuficiente de " + produto.getNome() + ". Disponível: "
						+ (produto.getQuantidade() - jaNoPedido));
				continue;
			}

			pedido.adicionarItem(new ItemPedido(produtoId, qtd, produto.getPreco()));
			Console.ok(qtd + "x " + produto.getNome() + " | total parcial: " + Console.moeda(pedido.getTotal()));
		}

		if (pedido.getItens().isEmpty()) {
			Console.aviso("Pedido cancelado: nenhum produto foi adicionado.");
			return;
		}

		mostrarResumoDoPedido(pedido);
		String confirma = Console.lerTexto(scanner, "Confirmar a venda? (S/N): ");
		if (!confirma.equalsIgnoreCase("S")) {
			Console.aviso("Venda cancelada. Nada foi alterado no estoque.");
			LogAuditoria.registrar(usuarioAtual, "VENDA_CANCELADA", "Pedido=" + pedido.getId());
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
		LogAuditoria.registrar(usuarioAtual, "VENDA_PEDIDO", "Pedido=" + pedido.getId() + ", Cliente="
				+ cliente.getId() + ", Itens=" + pedido.getItens().size() + ", Total=" + pedido.getTotal());

		Console.ok(pedido.getId() + " finalizado! Total: " + Console.moeda(pedido.getTotal()));
		System.out.println("Título a receber gerado: " + titulo.getId());
	}

	private void mostrarResumoDoPedido(Pedido pedido) {
		Console.titulo("Resumo do " + pedido.getId());
		System.out.printf("%-6s %-32s %5s %13s %13s%n", "ID", "PRODUTO", "QTD", "UNITÁRIO", "SUBTOTAL");
		Console.linha();
		for (ItemPedido item : pedido.getItens()) {
			Produto produto = buscarProdutoPorId(item.getProdutoId());
			String nome = (produto != null) ? produto.getNome() : "(produto não encontrado)";
			System.out.printf("%-6s %-32s %5d %13s %13s%n", item.getProdutoId(), cortar(nome, 32),
					item.getQuantidade(), Console.moeda(item.getPrecoUnitario()), Console.moeda(item.getSubtotal()));
		}
		Console.linha();
		System.out.printf("%-45s %13s %13s%n", "TOTAL DO PEDIDO", "", Console.moeda(pedido.getTotal()));
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

		// um ponto para CADA dia, da primeira venda até a última data relevante; dia sem venda vale 0
		LocalDate primeiroDia = vendasPorDia.keySet().iterator().next();
		LocalDate ultimoDiaComVenda = Collections.max(vendasPorDia.keySet());
		LocalDate hoje = LocalDate.now();
		LocalDate fimIntervalo = hoje.isAfter(ultimoDiaComVenda) ? hoje : ultimoDiaComVenda;
		List<Double> xs = new ArrayList<>();
		List<Double> ys = new ArrayList<>();
		for (LocalDate dia = primeiroDia; !dia.isAfter(fimIntervalo); dia = dia.plusDays(1)) {
			xs.add((double) ChronoUnit.DAYS.between(primeiroDia, dia));
			ys.add((double) vendasPorDia.getOrDefault(dia, 0));
		}

		double[] coeficientes = regressaoLinear(xs, ys);
		double m = coeficientes[0];
		double b = coeficientes[1];

		long proximoDiaIndice = ChronoUnit.DAYS.between(primeiroDia, LocalDate.now()) + 1;
		double previsao = Math.max(0, m * proximoDiaIndice + b);

		if (Math.abs(m) < 0.01) {
			System.out.println("Tendência de vendas: estável.");
		} else {
			String tendencia = m > 0 ? "crescimento" : "queda";
			System.out.println("Tendência de vendas: " + tendencia + " de aproximadamente "
					+ String.format("%.2f", Math.abs(m)) + " unidades/dia.");
		}
		System.out.println("Previsão de demanda para amanhã: " + String.format("%.1f", previsao) + " unidades.");
	}

	private double[] regressaoLinear(List<Double> xs, List<Double> ys) {
		int n = xs.size();
		if (n < 2) {
			return new double[]{0.0, 0.0};
		}

		double somaX = 0.0;
		double somaY = 0.0;
		double somaXY = 0.0;
		double somaX2 = 0.0;

		for (int i = 0; i < n; i++) {
			double x = xs.get(i);
			double y = ys.get(i);
			somaX += x;
			somaY += y;
			somaXY += x * y;
			somaX2 += x * x;
		}

		double denominador = n * somaX2 - somaX * somaX;
		if (Math.abs(denominador) < 1e-10) {
			return new double[]{0.0, somaY / n};
		}

		double m = (n * somaXY - somaX * somaY) / denominador;
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
			if (titulo.isPago()) {
				System.out.println("Título já está pago. Pagamento repetido não realizado.");
				return;
			}
			titulo.setPaga(true);
			saveTitulos();
			LogAuditoria.registrar(usuarioAtual, "PAGAMENTO", "Titulo=" + tituloId);
			System.out.println("Título pago com sucesso.");
		} else {
			System.out.println("Título não encontrado.");
		}
	}

	public void relatorioInventario() {
		Console.titulo("Relatório de inventário");
		if (produtos.isEmpty()) {
			System.out.println("Nenhum produto cadastrado ainda.");
			return;
		}
		List<Produto> ordenados = new ArrayList<>(produtos);
		ordenados.sort((a, b) -> a.getQuantidade() - b.getQuantidade());

		System.out.printf("%-6s %-32s %8s %15s %s%n", "ID", "PRODUTO", "QTD", "VALOR", "SITUAÇÃO");
		Console.linha();
		double valorTotal = 0;
		int emAlerta = 0;
		int zerados = 0;
		for (Produto p : ordenados) {
			double valorEmEstoque = p.getPreco() * p.getQuantidade();
			valorTotal += valorEmEstoque;
			String situacao = "OK";
			if (p.getQuantidade() == 0) {
				situacao = "ZERADO - repor já";
				zerados++;
				emAlerta++;
			} else if (p.getQuantidade() <= ESTOQUE_MINIMO) {
				situacao = "BAIXO";
				emAlerta++;
			}
			System.out.printf("%-6s %-32s %8d %15s %s%n", p.getId(), cortar(p.getNome(), 32),
					p.getQuantidade(), Console.moeda(valorEmEstoque), situacao);
		}
		Console.linha();
		System.out.println("Produtos cadastrados.......: " + produtos.size());
		System.out.println("Precisam de reposição......: " + emAlerta + " (sendo " + zerados + " zerado(s))");
		System.out.println("Valor total do inventário..: " + Console.moeda(valorTotal));
		LogAuditoria.registrar(usuarioAtual, "RELATORIO_INVENTARIO", "Produtos=" + produtos.size() + ", Alertas=" + emAlerta);
	}

	public void listarTitulosDeDestaque() {
		System.out.println("Títulos em Aberto:");
		boolean encontrouTituloAberto = false;

		for (Titulo title : titulos) {
			if (!title.isPago()) {
				encontrouTituloAberto = true;
				System.out.println(title.getId()
						+ " - R$ " + String.format("%.2f", title.getQuantidade())
						+ " - Pessoa: " + title.getPessoaId()
						+ " - Tipo: " + title.getTipoTitulo());
			}
		}

		if (!encontrouTituloAberto) {
			System.out.println("Nenhum título em aberto.");
		}
	}

	public void listarPedidos(Scanner scanner) {
		Console.titulo("Pedidos");
		if (pedidos.isEmpty()) {
			System.out.println("Nenhum pedido registrado.");
			return;
		}
		for (Pedido pedido : pedidos) {
			System.out.println(pedido.getId() + " | " + pedido.getData() + " | cliente ID "
					+ pedido.getClienteId() + " | " + pedido.getItens().size() + " item(ns) | "
					+ Console.moeda(pedido.getTotal()));
			for (ItemPedido item : pedido.getItens()) {
				Produto produto = buscarProdutoPorId(item.getProdutoId());
				String nome = (produto != null) ? produto.getNome() : "(produto não encontrado)";
				System.out.println("    " + item.getQuantidade() + "x " + nome + " - " + Console.moeda(item.getSubtotal()));
			}
		}
	}

	public void testeDesempenho() {
		Console.aviso("Teste de desempenho ainda não implementado.");
	}

	private Pessoa buscarPessoaPorTipo(Scanner scanner, int tipo) {
		String rotulo = switch (tipo) {
			case 1 -> "cliente";
			case 2 -> "fornecedor";
			default -> "funcionário";
		};
		String id = Console.lerTexto(scanner, "ID do " + rotulo + ": ");
		for (Pessoa p : pessoas) {
			if (p.getId().equals(id) && p.getTipo() == tipo) {
				return p;
			}
		}
		return null;
	}

	private Pessoa buscarPessoaPorId(String id) {
		for (Pessoa p : pessoas) {
			if (p.getId().equals(id)) {
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
					if (!line.isBlank()) registrarProduto(Produto.fromString(line));
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
			usuarios.add(new Usuario("admin", Usuario.hash("admin123"), "Admin"));
			usuarios.add(new Usuario("operador", Usuario.hash("operador123"), "Operador"));
			saveUsuarios();
			System.out.println("Aviso: arquivo de usuários não encontrado. Usuários padrão recriados - avise o administrador.");
			LogAuditoria.registrar("SISTEMA", "USUARIOS_PADRAO_RECRIADOS", "usuarios.txt não existia na inicialização");
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
		System.out.print("Novo login (Nome corrido): ");
		String username = scanner.nextLine();

		for (Usuario u : usuarios) {
			if (u.getUsername().equals(username)) {
				System.out.println("Já existe um usuário com esse login.");
				return;
			}
		}

		String senha;

		while (true) {
			System.out.print("Senha (mínimo 6 caracteres): ");
			senha = scanner.nextLine();
			if (senha.length() >= 6) {
				break;
			}
			System.out.println("Senha inválida. A senha deve ter pelo menos 6 caracteres.");
		}

		int papel;
		while (true) {
			System.out.print("Papel (1-Admin, 2-Operador): ");
			if (scanner.hasNextInt()) {
				papel = scanner.nextInt();
				scanner.nextLine();
				if (papel >= 1 && papel <= 2) {
					break;
				}
			} else {
				scanner.nextLine();
			}
			System.out.println("Opção inválida. Digite 1 ou 2.");
		}

		String role = (papel == 1) ? "Admin" : "Operador";
		Usuario novo = new Usuario(username, Usuario.hash(senha), role);
		usuarios.add(novo);
		saveUsuarios();
		System.out.println("Usuário '" + username + "' cadastrado como " + role + ".");
	}

	private void registrarProduto(Produto produto) {
		produtos.add(produto);
	}

	private String cortar(String texto, int tamanho) {
		if (texto.length() <= tamanho) {
			return texto;
		}
		return texto.substring(0, tamanho - 3) + "...";
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
