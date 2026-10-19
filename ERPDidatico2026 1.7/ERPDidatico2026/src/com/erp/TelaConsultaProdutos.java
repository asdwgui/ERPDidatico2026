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

public class TelaConsultaProdutos extends JFrame {
    private final List<Produto> produtos;

    private final DefaultTableModel modelo = new DefaultTableModel(new String[] {
            Idioma.t("tela.col.id"), Idioma.t("tela.col.produto"),
            Idioma.t("tela.col.preco"), Idioma.t("tela.col.estoque") }, 0);

    private final JTextField campoBusca = new JTextField(22);
    private final JLabel rodape = new JLabel();

    public TelaConsultaProdutos(List<Produto> produtos) {
        this.produtos = produtos;
        setTitle(Idioma.t("tela.titulo"));

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(780, 480);
        setLocationRelativeTo(null);
        JTable tabela = new JTable(modelo);
        tabela.setAutoCreateRowSorter(true);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(new JLabel(Idioma.t("tela.buscar")));
        campoBusca.addActionListener(evento -> atualizarTabela());
        barra.add(campoBusca);
        JButton botaoFiltrar = new JButton(Idioma.t("tela.filtrar"));
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
                    Formato.moeda(produto.getPreco()),
                    produto.getQuantidade()
            });
        }
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        rodape.setText(Idioma.t("tela.rodape", encontrados.size(), produtos.size(), Formato.numero(ms, 1)));
    }

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
