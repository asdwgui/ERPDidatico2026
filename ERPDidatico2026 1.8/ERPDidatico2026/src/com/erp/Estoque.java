package com.erp;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class  Estoque {
    private final List<Produto> produtos;
    private final List<Titulo> titulos;
    private final List<Pessoa> pessoas;
    private final List<Usuario> usuarios;
    private final List<Pedido> pedidos;
    private final Map<String, Produto> indiceProdutos;
    private String usuarioAtual = "Desconhecido";

    public void setUsuarioAtual(String usuario) {
        this.usuarioAtual = usuario;
    }

    public void setModoLote(boolean ligado) {
        this.modoLote = ligado;
    }

    private static final String PRODUTOS_ARQUIVO = "produtos.txt";
    private static final String TITULOS_ARQUIVO = "titulos.txt";
    private static final String PESSOAS_ARQUIVO = "pessoas.txt";
    private static final String USUARIOS_ARQUIVO = "usuarios.txt";
    private static final String PEDIDOS_ARQUIVO = "pedidos.txt";
    private static final int ESTOQUE_MINIMO = 5;
    private static final int PEDIDOS_POR_PAGINA = 10;
    private static final int LINHAS_RELATORIO = 20;

    private boolean modoLote = false;

    private boolean inventarioMudou = true;
    private BigDecimal valorTotalCache = BigDecimal.ZERO;
    private int emAlertaCache = 0;
    private int zeradosCache = 0;
    private List<Produto> criticosCache = new ArrayList<>();

    public Estoque() throws IOException {
        produtos = new ArrayList<>();
        titulos = new ArrayList<>();
        pessoas = new ArrayList<>();
        usuarios = new ArrayList<>();
        pedidos = new ArrayList<>();
        indiceProdutos = new HashMap<>();
        carregaProduto();
        carregaTitulos();
        carregaPessoas();
        carregaUsuarios();
        carregaPedidos();
    }

    public void addPessoa(Scanner scanner) throws IOException {
        String id = Console.lerTexto(scanner, Idioma.t("pessoa.id"));

        if (buscarPessoaPorId(id) != null) {
            Console.aviso(Idioma.t("pessoa.id.duplicado", id));
            return;
        }

        String nome = Console.lerTexto(scanner, Idioma.t("pessoa.nome"));

        int tipo;
        while (true) {
            tipo = Console.lerInt(scanner, Idioma.t("pessoa.tipo"));
            if (tipo >= 1 && tipo <= 3) {
                break;
            }
            Console.aviso(Idioma.t("pessoa.opcao.invalida"));
        }

        int opcaoBase;
        while (true) {
            opcaoBase = Console.lerInt(scanner, Idioma.t("pessoa.baselegal"));
            if (opcaoBase >= 1 && opcaoBase <= 3) {
                break;
            }
            Console.aviso(Idioma.t("pessoa.opcao.invalida"));
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
        Console.ok(Idioma.t("pessoa.cadastrada"));
    }

    public void addProduto(Scanner scanner) throws IOException {
        Console.titulo(Idioma.t("produto.cadastro.titulo"));
        String id = Console.lerTexto(scanner, Idioma.t("produto.id"));
        if (id.isEmpty()) {
            Console.aviso(Idioma.t("produto.id.vazio"));
            return;
        }
        if (buscarProdutoPorId(id) != null) {
            Console.aviso(Idioma.t("produto.id.duplicado", id));
            return;
        }
        String nome = Console.lerTexto(scanner, Idioma.t("produto.nome"));
        if (nome.isEmpty() || nome.contains(",")) {
            Console.aviso(Idioma.t("produto.nome.invalido"));
            return;
        }
        BigDecimal preco = Console.lerDecimal(scanner, Idioma.t("produto.preco"));
        if (preco.signum() <= 0) {
            Console.aviso(Idioma.t("produto.preco.invalido"));
            return;
        }
        int quantidade = Console.lerInt(scanner, Idioma.t("produto.quantidade"));
        if (quantidade < 0) {
            Console.aviso(Idioma.t("produto.quantidade.invalida"));
            return;
        }

        // DESAFIO 8: pede a sugestao ao servico de classificacao antes de salvar.
        String categoria = perguntarCategoria(scanner, id, nome, preco);

        Produto produto = new Produto(id, nome, preco, quantidade, categoria);
        registrarProduto(produto);
        salvarAlteracoes();
        LogAuditoria.registrar(usuarioAtual, "CADASTRO_PRODUTO", "ID=" + id + ", Nome=" + nome + ", Qtd=" + quantidade);
        Console.ok(Idioma.t("produto.cadastrado", nome, Formato.moeda(preco), quantidade));
        if (!categoria.isEmpty()) {
            Console.ok(Idioma.t("ia.categoria.gravada", categoria));
        }
    }

    public void listaProdutos(Scanner scanner) {
        String filtro = Console.lerTexto(scanner, Idioma.t("produtos.filtro")).toLowerCase();
        Console.titulo(Idioma.t("produtos.titulo"));
        if (produtos.isEmpty()) {
            System.out.println(Idioma.t("produtos.vazio"));
            return;
        }
        System.out.printf("%-6s %-32s %14s %9s%n", Idioma.t("col.id"), Idioma.t("col.produto"),
                Idioma.t("col.preco"), Idioma.t("col.estoque"));
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
            System.out.println(Idioma.t("produtos.nenhum.filtro", filtro));
        } else {
            System.out.println(Idioma.t("produtos.exibidos", mostrados, produtos.size()));
        }
    }
    public void listaPessoas() {
        Console.titulo(Idioma.t("pessoas.titulo"));

        if (pessoas.isEmpty()) {
            System.out.println(Idioma.t("pessoas.vazio"));
            return;
        }

        for (Pessoa pessoa : pessoas) {
            System.out.println(Idioma.t("pessoa.linha", pessoa.getId(), pessoa.getNome(),
                    rotuloTipoPessoa(pessoa.getTipo()), rotuloBaseLegal(pessoa.getBaseLegal())));
        }
    }

    public void anonimizarPessoa(Scanner scanner) throws IOException {
        String id = Console.lerTexto(scanner, Idioma.t("pessoa.anonimizar.id"));

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
            Console.ok(Idioma.t("pessoa.anonimizada"));
        } else {
            Console.aviso(Idioma.t("pessoa.nao.encontrada"));
        }
    }

    public void compraProduto(Scanner scanner) throws IOException {
        String produtoId = Console.lerTexto(scanner, Idioma.t("compra.produto.id"));

        Produto produto = buscarProdutoPorId(produtoId);

        if (produto != null) {
            Pessoa fornecedor = buscarPessoaPorTipo(scanner, 2);
            if (fornecedor == null) {
                Console.aviso(Idioma.t("compra.fornecedor.nao.encontrado"));
                return;
            }

            int qtd = Console.lerInt(scanner, Idioma.t("compra.quantidade"));
            if (qtd <= 0) {
                Console.aviso(Idioma.t("compra.quantidade.invalida"));
                return;
            }

            BigDecimal custoUnitario = Console.lerDecimal(scanner, Idioma.t("compra.custo"));
            if (custoUnitario.signum() <= 0) {
                Console.aviso(Idioma.t("compra.custo.invalido"));
                return;
            }

            produto.adicionarEstoque(qtd);
            inventarioMudou = true;
            BigDecimal total = custoUnitario.multiply(BigDecimal.valueOf(qtd));

            Titulo titulo = new Titulo(UUID.randomUUID().toString(), total, false, fornecedor.getId(),
                    "a pagar");
            titulos.add(titulo);
            salvarAlteracoes();
            LogAuditoria.registrar(usuarioAtual, "COMPRA", "Produto=" + produtoId + ", Qtd=" + qtd
                    + ", Fornecedor=" + fornecedor.getId());
            Console.ok(Idioma.t("compra.registrada", produto.getQuantidade()));
            System.out.println(Idioma.t("compra.titulo.gerado", titulo.getId()));
        } else {
            Console.aviso(Idioma.t("produto.nao.encontrado"));
        }
    }

    public void vendaProduto(Scanner scanner) throws IOException {
        Console.titulo(Idioma.t("venda.titulo"));
        Pessoa cliente = buscarPessoaPorTipo(scanner, 1);
        if (cliente == null) {
            Console.aviso(Idioma.t("venda.cliente.nao.encontrado"));
            return;
        }

        String data = Formato.agoraParaArquivo();
        Pedido pedido = new Pedido("Pedido " + (pedidos.size() + 1), cliente.getId(), data);

        pedido = new Pedido("Pedido " + (pedidos.size() + 1), cliente.getId(), data);

        System.out.println(Idioma.t("venda.cabecalho", pedido.getId(), cliente.getId(),
                Formato.dataParaTela(data)));
        System.out.println(Idioma.t("venda.instrucao"));
        while (true) {
            String produtoId = Console.lerTexto(scanner, Idioma.t("venda.prompt"));
            if (produtoId.equalsIgnoreCase(Idioma.t("venda.letra.fim"))) {
                break;
            }
            if (produtoId.equalsIgnoreCase(Idioma.t("venda.letra.remover"))) {
                String idRemover = Console.lerTexto(scanner, Idioma.t("venda.remover.id"));
                if (pedido.removerItem(idRemover)) {
                    Console.ok(Idioma.t("venda.item.removido", Formato.moeda(pedido.getTotal())));
                } else {
                    Console.aviso(Idioma.t("venda.item.inexistente"));
                }
                continue;
            }

            Produto produto = buscarProdutoPorId(produtoId);
            if (produto == null) {
                Console.aviso(Idioma.t("venda.produto.nao.encontrado"));
                continue;
            }

            int qtd = Console.lerInt(scanner, Idioma.t("venda.quantidade", produto.getNome()));
            if (qtd <= 0) {
                Console.aviso(Idioma.t("venda.quantidade.invalida"));
                continue;
            }

            int jaNoPedido = pedido.quantidadeDoProduto(produtoId);
            if (qtd + jaNoPedido > produto.getQuantidade()) {
                Console.aviso(Idioma.t("venda.estoque.insuficiente", produto.getNome(),
                        produto.getQuantidade() - jaNoPedido));
                continue;
            }

            pedido.adicionarItem(new ItemPedido(produtoId, qtd, produto.getPreco()));
            Console.ok(Idioma.t("venda.item.adicionado", qtd, produto.getNome(),
                    Formato.moeda(pedido.getTotal())));
        }

        if (pedido.getItens().isEmpty()) {
            Console.aviso(Idioma.t("venda.sem.itens"));
            return;
        }

        mostrarResumoDoPedido(pedido);
        String confirma = Console.lerTexto(scanner, Idioma.t("venda.confirmar"));
        if (!confirma.equalsIgnoreCase(Idioma.t("venda.letra.sim"))) {
            Console.aviso(Idioma.t("venda.cancelada"));
            LogAuditoria.registrar(usuarioAtual, "VENDA_CANCELADA", "Pedido=" + pedido.getId());
            return;
        }

        Titulo titulo = fecharPedido(pedido, cliente);

        Console.ok(Idioma.t("venda.finalizada", pedido.getId(), Formato.moeda(pedido.getTotal())));
        System.out.println(Idioma.t("venda.titulo.gerado", titulo.getId()));
    }

    public Titulo fecharPedido(Pedido pedido, Pessoa cliente) throws IOException {
        for (ItemPedido item : pedido.getItens()) {
            Produto produto = buscarProdutoPorId(item.getProdutoId());
            if (produto == null || produto.getQuantidade() < item.getQuantidade()) {
                throw new IllegalStateException("Estoque insuficiente (ou produto inexistente) para "
                        + item.getProdutoId() + " no " + pedido.getId());
            }
        }

        for (ItemPedido item : pedido.getItens()) {
            Produto produto = buscarProdutoPorId(item.getProdutoId());
            produto.removerEstoque(item.getQuantidade());
            HistoricoVendas.registrarVenda(item.getProdutoId(), item.getQuantidade());
        }
        inventarioMudou = true;

        Titulo titulo = new Titulo(UUID.randomUUID().toString(), pedido.getTotal(), false, cliente.getId(), "a receber");
        titulos.add(titulo);
        pedidos.add(pedido);

        salvarAlteracoes();
        LogAuditoria.registrar(usuarioAtual, "VENDA_PEDIDO", "Pedido=" + pedido.getId() + ", Cliente="
                + cliente.getId() + ", Itens=" + pedido.getItens().size() + ", Total=" + pedido.getTotal());
        return titulo;
    }

    private void mostrarResumoDoPedido(Pedido pedido) {
        Console.titulo(Idioma.t("pedido.resumo.titulo", pedido.getId()));
        System.out.printf("%-6s %-32s %5s %13s %13s%n", Idioma.t("col.id"), Idioma.t("col.produto"),
                Idioma.t("col.qtd"), Idioma.t("col.unitario"), Idioma.t("col.subtotal"));
        Console.linha();
        for (ItemPedido item : pedido.getItens()) {
            Produto produto = buscarProdutoPorId(item.getProdutoId());
            String nome = (produto != null) ? produto.getNome() : Idioma.t("pedido.produto.sumiu");
            System.out.printf("%-6s %-32s %5d %13s %13s%n", item.getProdutoId(), cortar(nome, 32),
                    item.getQuantidade(), Formato.moeda(item.getPrecoUnitario()),
                    Formato.moeda(item.getSubtotal()));
        }
        Console.linha();
        System.out.printf("%-45s %13s %13s%n", Idioma.t("pedido.total"), "",
                Formato.moeda(pedido.getTotal()));
    }



    public void preverDemanda(Scanner scanner) throws IOException {
        String produtoId = Console.lerTexto(scanner, Idioma.t("previsao.id"));

        boolean produtoExiste = produtos.stream().anyMatch(p -> p.getId().equals(produtoId));
        if (!produtoExiste) {
            Console.aviso(Idioma.t("produto.nao.encontrado"));
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
                Console.aviso(Idioma.t("previsao.linha.invalida"));
            }
        }

        if (vendasPorDia.size() < 2) {
            System.out.println(Idioma.t("previsao.sem.historico"));
            return;
        }

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
            System.out.println(Idioma.t("previsao.estavel"));
        } else {
            String tendencia = Idioma.t(m > 0 ? "previsao.crescimento" : "previsao.queda");
            System.out.println(Idioma.t("previsao.tendencia", tendencia,
                    Formato.numero(Math.abs(m), 2)));
        }
        System.out.println(Idioma.t("previsao.amanha", Formato.numero(previsao, 1)));
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
        String tituloId = Console.lerTexto(scanner, Idioma.t("pagamento.id"));

        Titulo titulo = null;
        for (Titulo t : titulos) {
            if (t.getId().equals(tituloId)) {
                titulo = t;
                break;
            }
        }

        if (titulo != null) {
            if (titulo.isPago()) {
                Console.aviso(Idioma.t("pagamento.ja.pago"));
                return;
            }
            titulo.setPaga(true);
            salvarAlteracoes();
            LogAuditoria.registrar(usuarioAtual, "PAGAMENTO", "Titulo=" + tituloId);
            Console.ok(Idioma.t("pagamento.ok"));
        } else {
            Console.aviso(Idioma.t("pagamento.nao.encontrado"));
        }
    }

    public void relatorioInventario() {
        Console.titulo(Idioma.t("relatorio.titulo"));
        if (produtos.isEmpty()) {
            System.out.println(Idioma.t("relatorio.vazio"));
            return;
        }

        if (inventarioMudou) {
            recalcularResumo();
        }

        System.out.printf("%-6s %-32s %8s %15s %s%n", Idioma.t("col.id"), Idioma.t("col.produto"),
                Idioma.t("col.qtd"), Idioma.t("col.valor"), Idioma.t("col.situacao"));
        Console.linha();
        for (Produto p : criticosCache) {
            BigDecimal valorEmEstoque = p.getPreco().multiply(BigDecimal.valueOf(p.getQuantidade()));
            String situacao = Idioma.t("relatorio.situacao.ok");
            if (p.getQuantidade() == 0) {
                situacao = Idioma.t("relatorio.situacao.zerado");
            } else if (p.getQuantidade() <= ESTOQUE_MINIMO) {
                situacao = Idioma.t("relatorio.situacao.baixo");
            }
            System.out.printf("%-6s %-32s %8d %15s %s%n", p.getId(), cortar(p.getNome(), 32),
                    p.getQuantidade(), Formato.moeda(valorEmEstoque), situacao);
        }
        Console.linha();
        if (produtos.size() > criticosCache.size()) {
            System.out.println(Idioma.t("relatorio.criticos", criticosCache.size(), produtos.size()));
        }
        System.out.printf("%-30s %s%n", Idioma.t("relatorio.cadastrados"), produtos.size());
        System.out.printf("%-30s %s%n", Idioma.t("relatorio.reposicao"),
                Idioma.t("relatorio.reposicao.valor", emAlertaCache, zeradosCache));
        System.out.printf("%-30s %s%n", Idioma.t("relatorio.valor.total"),
                Formato.moeda(valorTotalCache));
        LogAuditoria.registrar(usuarioAtual, "RELATORIO_INVENTARIO", "Produtos=" + produtos.size() + ", Alertas=" + emAlertaCache);
    }

    private void recalcularResumo() {
        BigDecimal total = BigDecimal.ZERO;
        int alerta = 0;
        int zerados = 0;
        for (Produto p : produtos) {
            total = total.add(p.getPreco().multiply(BigDecimal.valueOf(p.getQuantidade())));
            if (p.getQuantidade() == 0) {
                zerados++;
                alerta++;
            } else if (p.getQuantidade() <= ESTOQUE_MINIMO) {
                alerta++;
            }
        }
        valorTotalCache = total;
        emAlertaCache = alerta;
        zeradosCache = zerados;

        List<Produto> ordenados = new ArrayList<>(produtos);
        ordenados.sort((a, b) -> Integer.compare(a.getQuantidade(), b.getQuantidade()));
        criticosCache = new ArrayList<>(ordenados.subList(0, Math.min(LINHAS_RELATORIO, ordenados.size())));
        inventarioMudou = false;
    }

    public void listarTitulosDeDestaque() {
        Console.titulo(Idioma.t("titulos.titulo"));
        boolean encontrouTituloAberto = false;

        for (Titulo title : titulos) {
            if (!title.isPago()) {
                encontrouTituloAberto = true;
                System.out.println(Idioma.t("titulos.linha", title.getId(),
                        Formato.moeda(title.getQuantidade()), title.getPessoaId(),
                        rotuloTipoTitulo(title.getTipoTitulo())));
            }
        }

        if (!encontrouTituloAberto) {
            System.out.println(Idioma.t("titulos.vazio"));
        }
    }

    public void listarPedidos(Scanner scanner) {
        Console.titulo(Idioma.t("pedidos.titulo"));
        if (pedidos.isEmpty()) {
            System.out.println(Idioma.t("pedidos.vazio"));
            return;
        }
        int totalPaginas = (pedidos.size() + PEDIDOS_POR_PAGINA - 1) / PEDIDOS_POR_PAGINA;
        for (int pagina = 0; pagina < totalPaginas; pagina++) {
            for (int i = 0; i < PEDIDOS_POR_PAGINA; i++) {
                int posicao = pedidos.size() - 1 - (pagina * PEDIDOS_POR_PAGINA + i);
                if (posicao < 0) {
                    break;
                }
                Pedido pedido = pedidos.get(posicao);
                System.out.println(Idioma.t("pedidos.linha", pedido.getId(),
                        Formato.dataParaTela(pedido.getData()), pedido.getClienteId(),
                        pedido.getItens().size(), Formato.moeda(pedido.getTotal())));
                for (ItemPedido item : pedido.getItens()) {
                    Produto produto = buscarProdutoPorId(item.getProdutoId());
                    String nome = (produto != null) ? produto.getNome() : Idioma.t("pedido.produto.sumiu");
                    System.out.println(Idioma.t("pedidos.item", item.getQuantidade(), nome,
                            Formato.moeda(item.getSubtotal())));
                }
            }
            Console.linha();
            System.out.println(Idioma.t("pedidos.pagina", pagina + 1, totalPaginas, pedidos.size()));
            if (pagina < totalPaginas - 1) {
                String opcao = Console.lerTexto(scanner, Idioma.t("pedidos.proxima"));
                if (opcao.equalsIgnoreCase(Idioma.t("pedidos.letra.fim"))) {
                    return;
                }
            }
        }
    }

    public void testeDesempenho() {
        int totalProdutos = 50000;
        int totalBuscas = 5000;

        Console.titulo(Idioma.t("desempenho.titulo"));
        System.out.println(Idioma.t("desempenho.gerando", totalProdutos));

        List<Produto> lista = new ArrayList<>();
        Map<String, Produto> indice = new HashMap<>();
        for (int i = 1; i <= totalProdutos; i++) {
            Produto p = new Produto("P" + i, "Produto de teste " + i, new BigDecimal("10.00"), 100);
            lista.add(p);
            indice.put(p.getId(), p);
        }

        Random sorteio = new Random(1);
        List<String> idsProcurados = new ArrayList<>();
        for (int i = 0; i < totalBuscas; i++) {
            idsProcurados.add("P" + (sorteio.nextInt(totalProdutos) + 1));
        }

        int achadosLista = 0;
        long inicio = System.nanoTime();
        for (String id : idsProcurados) {
            for (Produto p : lista) {
                if (p.getId().equals(id)) {
                    achadosLista++;
                    break;
                }
            }
        }
        double msLista = (System.nanoTime() - inicio) / 1_000_000.0;

        int achadosIndice = 0;
        inicio = System.nanoTime();
        for (String id : idsProcurados) {
            if (indice.get(id) != null) {
                achadosIndice++;
            }
        }
        double msIndice = (System.nanoTime() - inicio) / 1_000_000.0;

        inicio = System.nanoTime();
        String texto = "";
        for (int i = 0; i < 20000; i++) {
            texto += i + ";";
        }
        double msConcat = (System.nanoTime() - inicio) / 1_000_000.0;

        inicio = System.nanoTime();
        StringBuilder construtor = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            construtor.append(i).append(";");
        }
        double msBuilder = (System.nanoTime() - inicio) / 1_000_000.0;

        Console.linha();
        System.out.printf("%-46s %10.1f ms%n", Idioma.t("desempenho.lista", totalBuscas), msLista);
        System.out.printf("%-46s %10.1f ms%n", Idioma.t("desempenho.indice", totalBuscas), msIndice);
        System.out.printf("%-46s %10.1f ms%n", Idioma.t("desempenho.concat"), msConcat);
        System.out.printf("%-46s %10.1f ms%n", Idioma.t("desempenho.builder"), msBuilder);
        Console.linha();
        System.out.println(Idioma.t("desempenho.conferencia", achadosLista, achadosIndice));
        System.out.println(Idioma.t("desempenho.tamanhos", texto.length(), construtor.length()));

        LogAuditoria.registrar(usuarioAtual, "TESTE_DESEMPENHO", "Lista=" + String.format("%.1f", msLista)
                + "ms, Indice=" + String.format("%.1f", msIndice) + "ms");
    }

    private Pessoa buscarPessoaPorTipo(Scanner scanner, int tipo) {
        String id = Console.lerTexto(scanner, Idioma.t("pessoa.pedir." + tipo));
        for (Pessoa p : pessoas) {
            if (p.getId().equals(id) && p.getTipo() == tipo) {
                return p;
            }
        }
        return null;
    }

    public Pessoa buscarPessoaPorId(String id) {
        for (Pessoa p : pessoas) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public Produto buscarProdutoPorId(String id) {
        return indiceProdutos.get(id);
    }

    public List<Produto> getProdutos() {
        return new ArrayList<>(produtos);
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
            usuarios.add(new Usuario("admin", Usuario.hash("admin123"), "Admin", "pt"));
            usuarios.add(new Usuario("operador", Usuario.hash("operador123"), "Operador", "pt"));
            saveUsuarios();
            System.out.println(Idioma.t("usuarios.recriados"));
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
        String username = Console.lerTexto(scanner, Idioma.t("usuario.novo.login"));

        for (Usuario u : usuarios) {
            if (u.getUsername().equals(username)) {
                Console.aviso(Idioma.t("usuario.duplicado"));
                return;
            }
        }

        String senha;

        while (true) {
            senha = Console.lerTexto(scanner, Idioma.t("usuario.senha"));
            if (senha.length() >= 6) {
                break;
            }
            Console.aviso(Idioma.t("usuario.senha.curta"));
        }

        int papel;
        while (true) {
            papel = Console.lerInt(scanner, Idioma.t("usuario.papel"));
            if (papel >= 1 && papel <= 2) {
                break;
            }
            Console.aviso(Idioma.t("usuario.papel.invalido"));
        }

        String role = (papel == 1) ? "Admin" : "Operador";
        Usuario novo = new Usuario(username, Usuario.hash(senha), role, Idioma.atual());
        usuarios.add(novo);
        saveUsuarios();
        Console.ok(Idioma.t("usuario.cadastrado", username, role));
    }

    public void trocarIdioma(Scanner scanner, Usuario logado) throws IOException {
        Console.titulo(Idioma.t("idioma.titulo"));
        System.out.println(Idioma.t("idioma.atual", Idioma.t("idioma.nome")));
        System.out.println(Idioma.t("idioma.disponiveis"));
        String codigo = Console.lerTexto(scanner, Idioma.t("idioma.escolha"));
        if (codigo.isEmpty()) {
            return;
        }

        Idioma.usar(codigo.toLowerCase());
        logado.setIdioma(Idioma.atual());
        saveUsuarios();

        Console.ok(Idioma.t("idioma.alterado", Idioma.t("idioma.nome")));
        System.out.println(Idioma.t("idioma.salvo", logado.getUsername()));
        LogAuditoria.registrar(usuarioAtual, "TROCA_IDIOMA", "Novo=" + Idioma.atual());
    }

    private String rotuloTipoPessoa(int tipo) {
        return Idioma.t("pessoa.tipo." + tipo);
    }

    private String rotuloBaseLegal(String baseLegal) {
        return switch (baseLegal) {
            case "Consentimento" -> Idioma.t("pessoa.baselegal.1");
            case "Execução de Contrato" -> Idioma.t("pessoa.baselegal.2");
            case "Obrigação Legal" -> Idioma.t("pessoa.baselegal.3");
            default -> Idioma.t("pessoa.baselegal.0");
        };
    }

    private String rotuloTipoTitulo(String tipo) {
        return "a receber".equals(tipo) ? Idioma.t("titulo.tipo.receber") : Idioma.t("titulo.tipo.pagar");
    }

    private String perguntarCategoria(Scanner scanner, String id, String nome, BigDecimal preco) {
        String sugerida = "";
        double confianca = 0;

        try {
            ServicoClassificacao.Resposta resposta = ServicoClassificacao.classificarProduto(nome, preco);
            sugerida = resposta.sugestao;
            confianca = resposta.confianca;
            Console.ok(Idioma.t("ia.sugestao", sugerida,
                    Formato.numero(confianca * 100, 0), resposta.ms));
        } catch (ServicoClassificacao.ServicoIndisponivelException e) {
            Console.aviso(Idioma.t("ia.indisponivel", e.getMessage()));
            MetricasClassificacao.registrarFalha("PRODUTO", id, e.getMessage());
            return "";
        }

        String escolhida = Console.lerTexto(scanner, Idioma.t("ia.confirmar"));
        if (escolhida.isEmpty()) {
            escolhida = sugerida;
        }
        MetricasClassificacao.registrar("PRODUTO", id, sugerida, confianca, escolhida);
        return escolhida;
    }

    public void cadastrarProduto(String id, String nome, BigDecimal preco, int quantidade,
                                 String categoria) throws IOException {
        Produto produto = new Produto(id, nome, preco, quantidade, categoria);
        registrarProduto(produto);
        salvarAlteracoes();
        LogAuditoria.registrar(usuarioAtual, "CADASTRO_PRODUTO",
                "ID=" + id + ", Nome=" + nome + ", Categoria=" + categoria);
    }

    public void definirSegmento(String pessoaId, String segmento) throws IOException {
        Pessoa pessoa = buscarPessoaPorId(pessoaId);
        if (pessoa == null) {
            return;
        }
        pessoa.setSegmento(segmento);
        savePessoas();
        LogAuditoria.registrar(usuarioAtual, "SEGMENTO_CLIENTE",
                "ID=" + pessoaId + ", Segmento=" + segmento);
    }

    public double[] historicoDoCliente(String pessoaId) {
        int quantos = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Pedido pedido : pedidos) {
            if (pedido.getClienteId().equals(pessoaId)) {
                quantos++;
                total = total.add(pedido.getTotal());
            }
        }
        return new double[] { quantos, total.doubleValue() };
    }

    public List<Pessoa> getClientes() {
        List<Pessoa> clientes = new ArrayList<>();
        for (Pessoa p : pessoas) {
            if (p.getTipo() == 1) {
                clientes.add(p);
            }
        }
        return clientes;
    }

    private void registrarProduto(Produto produto) {
        produtos.add(produto);
        indiceProdutos.put(produto.getId(), produto);
        inventarioMudou = true;
    }

    private String cortar(String texto, int tamanho) {
        if (texto.length() <= tamanho) {
            return texto;
        }
        return texto.substring(0, tamanho - 3) + "...";
    }

    private void salvarAlteracoes() throws IOException {
        if (modoLote) {
            return;
        }
        saveProdutos();
        saveTitulos();
        savePedidos();
    }

    public void salvarTudo() throws IOException {
        saveProdutos();
        saveTitulos();
        savePedidos();
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