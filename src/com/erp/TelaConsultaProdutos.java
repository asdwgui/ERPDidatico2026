package com.erp;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
// DESAFIO 6: prova de conceito da nova interface (Java Swing).
// Mostra os MESMOS produtos do console, mas com busca instantânea e ordenação por coluna.
public class TelaConsultaProdutos extends JFrame {
    private final List<Produto> produtos;
    private final DefaultTableModel modelo =
            new DefaultTableModel(new String[] { "ID", "Produto", "Preço", "Estoque" }, 0);
    private final JTextField campoBusca = new JTextField(22);
    private final JLabel rodape = new JLabel();
    public TelaConsultaProdutos(List<Produto> produtos) {
        this.produtos = produtos;
        setTitle("ERP Didático 2026 - Consulta de produtos (protótipo)");
// DISPOSE_ON_CLOSE: fechar a janela não encerra o ERP que está rodando no console
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(780, 480);
        setLocationRelativeTo(null);
        JTable tabela = new JTable(modelo);
        tabela.setAutoCreateRowSorter(true); // clicar no cabeçalho ordena a coluna
        add(new JScrollPane(tabela), BorderLayout.CENTER); // sem o JScrollPane o cabeçalho some
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(new JLabel("Buscar por nome ou ID:"));
        campoBusca.addActionListener(evento -> atualizarTabela()); // Enter também filtra
        barra.add(campoBusca);
        JButton botaoFiltrar = new JButton("Filtrar");
        botaoFiltrar.addActionListener(evento -> atualizarTabela());
        barra.add(botaoFiltrar);
        add(barra, BorderLayout.NORTH);
        add(rodape, BorderLayout.SOUTH);
        atualizarTabela();
    }
    private void atualizarTabela() {
        long inicio = System.nanoTime();
        List<Produto> encontrados = filtrar(produtos, campoBusca.getText());
        modelo.setRowCount(0);
        for (Produto produto : encontrados) {
            modelo.addRow(new Object[] {
                    produto.getId(),
                    produto.getNome(),
                    Console.moeda(produto.getPreco()),
                    produto.getQuantidade()
            });
        }
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        rodape.setText(String.format(" %d de %d produtos | busca e montagem da tabela: %.1f ms",
                encontrados.size(), produtos.size(), ms));
    }
    // Fica separado da janela de propósito: assim a busca pode ser testada sem abrir a tela.
    public static List<Produto> filtrar(List<Produto> produtos, String texto) {
        String filtro = (texto == null) ? "" : texto.trim().toLowerCase();
        List<Produto> resultado = new ArrayList<>();
        for (Produto produto : produtos) {
            if (filtro.isEmpty()
                    || produto.getNome().toLowerCase().contains(filtro)
                    || produto.getId().toLowerCase().contains(filtro)) {
                resultado.add(produto);
            }
        }
        return resultado;
    }
}
