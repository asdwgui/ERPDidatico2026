package com.erp;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class TelaCadastroInteligente extends JFrame {

    private final Estoque estoque;

    private final JTextField produtoId = new JTextField(10);
    private final JTextField produtoNome = new JTextField(22);
    private final JTextField produtoPreco = new JTextField(10);
    private final JTextField produtoQuantidade = new JTextField(6);
    private final JComboBox<String> produtoCategoria =
            new JComboBox<>(categoriasExibidas());
    private final JLabel produtoAviso = new JLabel(" ");
    private String produtoSugerida = "";
    private double produtoConfianca = 0;

    private final JTextField clienteId = new JTextField(10);
    private final JTextField clienteNome = new JTextField(22);
    private final JTextField clientePedidos = new JTextField(6);
    private final JTextField clienteValor = new JTextField(10);
    private final JComboBox<String> clienteSegmento =
            new JComboBox<>(segmentosExibidos());
    private final JLabel clienteAviso = new JLabel(" ");
    private String clienteSugerido = "";
    private double clienteConfianca = 0;

    public TelaCadastroInteligente(Estoque estoque) {
        this.estoque = estoque;
        setTitle(Idioma.t("ia.tela.titulo"));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 330);
        setLocationRelativeTo(null);

        produtoCategoria.setEditable(true);
        clienteSegmento.setEditable(true);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab(Idioma.t("ia.tela.aba.produto"), painelProduto());
        abas.addTab(Idioma.t("ia.tela.aba.cliente"), painelCliente());
        add(abas, BorderLayout.CENTER);
    }

    private JPanel painelProduto() {
        JPanel campos = new JPanel(new GridLayout(5, 2, 6, 6));
        campos.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        campos.add(new JLabel(Idioma.t("produto.id")));
        campos.add(produtoId);
        campos.add(new JLabel(Idioma.t("produto.nome")));
        campos.add(produtoNome);
        campos.add(new JLabel(Idioma.t("produto.preco")));
        campos.add(produtoPreco);
        campos.add(new JLabel(Idioma.t("produto.quantidade")));
        campos.add(produtoQuantidade);
        campos.add(new JLabel(Idioma.t("ia.categoria")));
        campos.add(produtoCategoria);

        JButton sugerir = new JButton(Idioma.t("ia.botao.sugerir"));
        sugerir.addActionListener(e -> sugerirCategoria());
        JButton salvar = new JButton(Idioma.t("ia.botao.salvar"));
        salvar.addActionListener(e -> salvarProduto());

        JPanel botoes = new JPanel();
        botoes.add(sugerir);
        botoes.add(salvar);

        JPanel painel = new JPanel(new BorderLayout());
        painel.add(campos, BorderLayout.CENTER);
        painel.add(rodape(botoes, produtoAviso), BorderLayout.SOUTH);
        return painel;
    }

    private void sugerirCategoria() {
        String nome = produtoNome.getText().trim();
        BigDecimal preco = lerPreco(produtoPreco.getText());
        if (nome.isEmpty() || preco == null) {
            avisar(produtoAviso, Idioma.t("ia.falta.dados"), false);
            return;
        }
        try {
            ServicoClassificacao.Resposta resposta =
                    ServicoClassificacao.classificarProduto(nome, preco);
            produtoSugerida = resposta.sugestao;
            produtoConfianca = resposta.confianca;
            produtoCategoria.setSelectedItem(opcaoExibida(resposta.sugestao));
            avisar(produtoAviso, Idioma.t("ia.sugestao", opcaoExibida(resposta.sugestao),
                    Formato.numero(resposta.confianca * 100, 0), resposta.ms), true);
        } catch (ServicoClassificacao.ServicoIndisponivelException ex) {
            produtoSugerida = "";
            produtoConfianca = 0;
            MetricasClassificacao.registrarFalha("PRODUTO", produtoId.getText().trim(),
                    ex.getMessage());
            avisar(produtoAviso, Idioma.t("ia.indisponivel", ex.getMessage()), false);
        }
    }

    private void salvarProduto() {
        String id = produtoId.getText().trim();
        String nome = produtoNome.getText().trim();
        BigDecimal preco = lerPreco(produtoPreco.getText());
        if (id.isEmpty() || nome.isEmpty() || nome.contains(",") || preco == null) {
            avisar(produtoAviso, Idioma.t("ia.falta.dados"), false);
            return;
        }
        if (estoque.buscarProdutoPorId(id) != null) {
            avisar(produtoAviso, Idioma.t("produto.id.duplicado", id), false);
            return;
        }
        int quantidade;
        try {
            quantidade = Integer.parseInt(produtoQuantidade.getText().trim());
        } catch (NumberFormatException e) {
            avisar(produtoAviso, Idioma.t("ia.falta.dados"), false);
            return;
        }

        String escolhidaExibida = String.valueOf(produtoCategoria.getSelectedItem()).trim();
        String escolhida = opcaoCanonica(escolhidaExibida);
        try {
            estoque.cadastrarProduto(id, nome, preco, quantidade, escolhida);
        } catch (Exception e) {
            avisar(produtoAviso, e.getMessage(), false);
            return;
        }

        if (!produtoSugerida.isEmpty()) {
            MetricasClassificacao.registrar("PRODUTO", id, produtoSugerida,
                    produtoConfianca, escolhida);
        }

        JOptionPane.showMessageDialog(this,
                Idioma.t("ia.salvo.produto", nome, escolhidaExibida.isEmpty() ? "-" : escolhidaExibida));
        produtoId.setText("");
        produtoNome.setText("");
        produtoPreco.setText("");
        produtoQuantidade.setText("");
        produtoSugerida = "";
        avisar(produtoAviso, " ", true);
    }

    private JPanel painelCliente() {
        JPanel campos = new JPanel(new GridLayout(5, 2, 6, 6));
        campos.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        campos.add(new JLabel(Idioma.t("pessoa.id")));
        campos.add(clienteId);
        campos.add(new JLabel(Idioma.t("pessoa.nome")));
        campos.add(clienteNome);
        campos.add(new JLabel(Idioma.t("ia.cliente.pedidos")));
        campos.add(clientePedidos);
        campos.add(new JLabel(Idioma.t("ia.cliente.valor")));
        campos.add(clienteValor);
        campos.add(new JLabel(Idioma.t("ia.segmento")));
        campos.add(clienteSegmento);

        JButton buscar = new JButton(Idioma.t("ia.botao.historico"));
        buscar.addActionListener(e -> carregarHistorico());
        JButton sugerir = new JButton(Idioma.t("ia.botao.sugerir"));
        sugerir.addActionListener(e -> sugerirSegmento());
        JButton salvar = new JButton(Idioma.t("ia.botao.salvar"));
        salvar.addActionListener(e -> salvarSegmento());

        JPanel botoes = new JPanel();
        botoes.add(buscar);
        botoes.add(sugerir);
        botoes.add(salvar);

        JPanel painel = new JPanel(new BorderLayout());
        painel.add(campos, BorderLayout.CENTER);
        painel.add(rodape(botoes, clienteAviso), BorderLayout.SOUTH);
        return painel;
    }

    private void carregarHistorico() {
        String id = clienteId.getText().trim();
        Pessoa pessoa = estoque.buscarPessoaPorId(id);
        if (pessoa == null) {
            avisar(clienteAviso, Idioma.t("pessoa.nao.encontrada"), false);
            return;
        }
        double[] historico = estoque.historicoDoCliente(id);
        clienteNome.setText(pessoa.getNome());
        clientePedidos.setText(String.valueOf((int) historico[0]));
        clienteValor.setText(String.format(java.util.Locale.US, "%.2f", historico[1]));
        if (!pessoa.getSegmento().isEmpty()) {
            clienteSegmento.setSelectedItem(opcaoExibida(pessoa.getSegmento()));
        }
        avisar(clienteAviso, Idioma.t("ia.historico.carregado",
                (int) historico[0], Formato.moeda(BigDecimal.valueOf(historico[1]))), true);
    }

    private void sugerirSegmento() {
        int pedidos;
        double valor;
        try {
            pedidos = Integer.parseInt(clientePedidos.getText().trim());
            valor = Double.parseDouble(clienteValor.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            avisar(clienteAviso, Idioma.t("ia.falta.dados"), false);
            return;
        }
        try {
            ServicoClassificacao.Resposta resposta =
                    ServicoClassificacao.classificarCliente(pedidos, valor);
            clienteSugerido = resposta.sugestao;
            clienteConfianca = resposta.confianca;
            clienteSegmento.setSelectedItem(opcaoExibida(resposta.sugestao));
            avisar(clienteAviso, Idioma.t("ia.sugestao", opcaoExibida(resposta.sugestao),
                    Formato.numero(resposta.confianca * 100, 0), resposta.ms), true);
            if (pedidos == 0) {
                avisar(clienteAviso, Idioma.t("ia.cliente.sem.historico"), false);
            }
        } catch (ServicoClassificacao.ServicoIndisponivelException ex) {
            clienteSugerido = "";
            MetricasClassificacao.registrarFalha("CLIENTE", clienteId.getText().trim(),
                    ex.getMessage());
            avisar(clienteAviso, Idioma.t("ia.indisponivel", ex.getMessage()), false);
        }
    }

    private void salvarSegmento() {
        String id = clienteId.getText().trim();
        if (estoque.buscarPessoaPorId(id) == null) {
            avisar(clienteAviso, Idioma.t("pessoa.nao.encontrada"), false);
            return;
        }
        String escolhidoExibido = String.valueOf(clienteSegmento.getSelectedItem()).trim();
        String escolhido = opcaoCanonica(escolhidoExibido);
        try {
            estoque.definirSegmento(id, escolhido);
        } catch (Exception e) {
            avisar(clienteAviso, e.getMessage(), false);
            return;
        }
        if (!clienteSugerido.isEmpty()) {
            MetricasClassificacao.registrar("CLIENTE", id, clienteSugerido,
                    clienteConfianca, escolhido);
        }
        JOptionPane.showMessageDialog(this, Idioma.t("ia.salvo.cliente",
                clienteNome.getText().trim(), escolhidoExibido));
        clienteSugerido = "";
    }

    private static String[] categoriasExibidas() {
        String[] valores = ExportadorAmostra.categorias();
        for (int i = 0; i < valores.length; i++) valores[i] = opcaoExibida(valores[i]);
        return valores;
    }

    private static String[] segmentosExibidos() {
        String[] valores = ExportadorAmostra.segmentos();
        for (int i = 0; i < valores.length; i++) valores[i] = opcaoExibida(valores[i]);
        return valores;
    }

    private static String opcaoExibida(String valor) {
        if (!"de".equalsIgnoreCase(Idioma.atual()) || valor == null) return valor;
        switch (valor.trim()) {
            case "Celular": return "Mobiltelefon";
            case "Notebook": return "Notebook";
            case "Monitor": return "Monitor";
            case "Periférico": return "Peripheriegeräte";
            case "Mobiliário": return "Möbel";
            case "Papelaria": return "Schreibwaren";
            case "Varejo": return "Einzelhandel";
            case "Atacado": return "Großhandel";
            case "Corporativo": return "Geschäftskunden";
            default: return valor; // preserva categorias personalizadas
        }
    }

    private static String opcaoCanonica(String valor) {
        if (!"de".equalsIgnoreCase(Idioma.atual()) || valor == null) return valor;
        switch (valor.trim()) {
            case "Mobiltelefon": return "Celular";
            case "Notebook": return "Notebook";
            case "Monitor": return "Monitor";
            case "Peripheriegeräte": return "Periférico";
            case "Möbel": return "Mobiliário";
            case "Schreibwaren": return "Papelaria";
            case "Einzelhandel": return "Varejo";
            case "Großhandel": return "Atacado";
            case "Geschäftskunden": return "Corporativo";
            default: return valor; // preserva valores personalizados
        }
    }

    private JPanel rodape(JPanel botoes, JLabel aviso) {
        aviso.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.add(botoes, BorderLayout.NORTH);
        rodape.add(aviso, BorderLayout.SOUTH);
        return rodape;
    }

    private void avisar(JLabel aviso, String texto, boolean bom) {
        aviso.setForeground(bom ? new Color(0x1F, 0x7A, 0x3F) : new Color(0xA3, 0x34, 0x1C));
        aviso.setText(texto);
    }

    private BigDecimal lerPreco(String texto) {
        try {
            BigDecimal digitado = new BigDecimal(texto.trim().replace(",", "."))
                    .setScale(2, RoundingMode.HALF_UP);
            if (digitado.signum() <= 0) {
                return null;
            }

            return Formato.paraArmazenar(digitado);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
