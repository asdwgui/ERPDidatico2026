package com.erp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class GestaoProjetos {

    private static final String PROJETOS_ARQUIVO = "projetos.txt";
    private static final String APROVACOES_ARQUIVO = "aprovacoes.txt";

    private final List<Projeto> projetos = new ArrayList<>();
    private final List<Aprovacao> aprovacoes = new ArrayList<>();
    private final Estoque estoque;
    private String usuarioAtual = "Desconhecido";

    public GestaoProjetos(Estoque estoque) throws IOException {
        this.estoque = estoque;
        carregarProjetos();
        carregarAprovacoes();
    }

    public void setUsuarioAtual(String usuario) {
        this.usuarioAtual = usuario;
    }

    public List<Projeto> getProjetos() {
        return new ArrayList<>(projetos);
    }

    public Projeto buscarProjeto(String id) {
        for (Projeto p : projetos) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    public Aprovacao buscarAprovacao(String id) {
        for (Aprovacao a : aprovacoes) {
            if (a.getId().equalsIgnoreCase(id)) {
                return a;
            }
        }
        return null;
    }

    public BigDecimal gastoDoProjeto(String projetoId) {
        BigDecimal total = BigDecimal.ZERO;
        for (Aprovacao a : aprovacoes) {
            if (a.getProjetoId().equals(projetoId) && a.isAprovada()) {
                total = total.add(a.getValor());
            }
        }
        return total;
    }

    public List<Aprovacao> aprovacoesDoProjeto(String projetoId) {
        List<Aprovacao> lista = new ArrayList<>();
        for (Aprovacao a : aprovacoes) {
            if (a.getProjetoId().equals(projetoId)) {
                lista.add(a);
            }
        }
        return lista;
    }

    public List<Aprovacao> pendentesPara(Usuario usuario) {
        List<Aprovacao> lista = new ArrayList<>();
        for (Aprovacao a : aprovacoes) {
            if (!a.isPendente()) {
                continue;
            }
            if (Permissoes.podeAprovar(usuario, a, buscarProjeto(a.getProjetoId())).permitido) {
                lista.add(a);
            }
        }
        return lista;
    }

    public void cadastrarProjeto(Scanner scanner, Usuario logado) throws IOException {
        Console.titulo(Idioma.t("projeto.cadastro.titulo"));
        if (!Permissoes.podeGerenciarProjetos(logado)) {
            Console.aviso(Idioma.t("perm.so.admin.projeto"));
            return;
        }

        String id = Console.lerTexto(scanner, Idioma.t("projeto.id"));
        if (id.isEmpty() || id.contains(",")) {
            Console.aviso(Idioma.t("projeto.id.invalido"));
            return;
        }
        if (buscarProjeto(id) != null) {
            Console.aviso(Idioma.t("projeto.id.duplicado", id));
            return;
        }

        String nome = Console.lerTexto(scanner, Idioma.t("projeto.nome"));
        if (nome.isEmpty() || nome.contains(",")) {
            Console.aviso(Idioma.t("projeto.nome.invalido"));
            return;
        }

        String lider = Console.lerTexto(scanner, Idioma.t("projeto.lider"));
        Usuario usuarioLider = estoque.buscarUsuario(lider);
        if (usuarioLider == null) {
            Console.aviso(Idioma.t("projeto.lider.inexistente", lider));
            return;
        }

        BigDecimal orcamento = Console.lerDecimal(scanner, Idioma.t("projeto.orcamento"));
        if (orcamento.signum() <= 0) {
            Console.aviso(Idioma.t("projeto.orcamento.invalido"));
            return;
        }

        if (!Permissoes.ADMIN.equals(usuarioLider.getRole())) {
            usuarioLider.setRole(Permissoes.LIDER);
            estoque.salvarUsuarios();
        }

        projetos.add(new Projeto(id, nome, lider, orcamento, Projeto.ATIVO, new ArrayList<>()));
        salvarProjetos();
        LogAuditoria.registrar(usuarioAtual, "CADASTRO_PROJETO",
                "ID=" + id + ", Lider=" + lider + ", Orcamento=" + orcamento);
        Console.ok(Idioma.t("projeto.cadastrado", nome, lider));
    }

    public void gerenciarEquipe(Scanner scanner, Usuario logado) throws IOException {
        Console.titulo(Idioma.t("projeto.equipe.titulo"));
        String projetoId = Console.lerTexto(scanner, Idioma.t("projeto.id"));
        Projeto projeto = buscarProjeto(projetoId);

        Permissoes.Resultado podeEntrar = Permissoes.podeGerenciarEquipe(logado, projeto);
        if (!podeEntrar.permitido) {
            Console.aviso(Idioma.t(podeEntrar.motivo));
            LogAuditoria.registrar(usuarioAtual, "ACESSO_NEGADO",
                    "Equipe do projeto " + projetoId + ": " + podeEntrar.motivo);
            return;
        }

        System.out.println(Idioma.t("projeto.equipe.atual",
                projeto.getMembros().isEmpty() ? "-" : String.join(", ", projeto.getMembros())));

        String login = Console.lerTexto(scanner, Idioma.t("projeto.membro.login"));
        if (login.isEmpty()) {
            return;
        }
        if (estoque.buscarUsuario(login) == null) {
            Console.aviso(Idioma.t("projeto.membro.inexistente", login));
            return;
        }

        if (projeto.getMembros().contains(login)) {
            projeto.removerMembro(login);
            salvarProjetos();
            LogAuditoria.registrar(usuarioAtual, "EQUIPE_PROJETO", "Removeu " + login + " de " + projetoId);
            Console.ok(Idioma.t("projeto.membro.removido", login));
            return;
        }

        projeto.adicionarMembro(login);
        Usuario membro = estoque.buscarUsuario(login);
        if (Permissoes.OPERADOR.equals(membro.getRole())) {
            membro.setRole(Permissoes.MEMBRO);
            estoque.salvarUsuarios();
        }
        salvarProjetos();
        LogAuditoria.registrar(usuarioAtual, "EQUIPE_PROJETO", "Adicionou " + login + " a " + projetoId);
        Console.ok(Idioma.t("projeto.membro.adicionado", login));
    }

    public void solicitar(Scanner scanner, Usuario logado) throws IOException {
        Console.titulo(Idioma.t("aprovacao.solicitar.titulo"));
        String projetoId = Console.lerTexto(scanner, Idioma.t("projeto.id"));
        Projeto projeto = buscarProjeto(projetoId);

        Permissoes.Resultado pode = Permissoes.podeSolicitar(logado, projeto);
        if (!pode.permitido) {
            Console.aviso(Idioma.t(pode.motivo));
            return;
        }

        int opcao = Console.lerInt(scanner, Idioma.t("aprovacao.tipo"));
        String tipo = (opcao == 2) ? Aprovacao.TIPO_COMPRA : Aprovacao.TIPO_ORCAMENTO;

        String descricao = Console.lerTexto(scanner, Idioma.t("aprovacao.descricao"));
        if (descricao.contains(",")) {
            descricao = descricao.replace(",", " ");
        }

        BigDecimal valor = Console.lerDecimal(scanner, Idioma.t("aprovacao.valor"));
        if (valor.signum() <= 0) {
            Console.aviso(Idioma.t("aprovacao.valor.invalido"));
            return;
        }

        Aprovacao nova = new Aprovacao(UUID.randomUUID().toString().substring(0, 8),
                projetoId, tipo, valor, logado.getUsername(), Aprovacao.PENDENTE,
                Formato.agoraParaArquivo(), "", descricao);
        aprovacoes.add(nova);
        salvarAprovacoes();
        LogAuditoria.registrar(usuarioAtual, "SOLICITACAO_APROVACAO",
                "ID=" + nova.getId() + ", Projeto=" + projetoId + ", Valor=" + valor);

        Console.ok(Idioma.t("aprovacao.criada", nova.getId(), projeto.getLider()));

        if (valor.compareTo(Permissoes.TETO_DO_LIDER) > 0) {
            Console.aviso(Idioma.t("aprovacao.acima.teto",
                    Formato.moeda(Permissoes.TETO_DO_LIDER)));
        }
    }

    public void decidir(Scanner scanner, Usuario logado) throws IOException {
        Console.titulo(Idioma.t("aprovacao.decidir.titulo"));

        List<Aprovacao> minhas = pendentesPara(logado);
        if (minhas.isEmpty()) {
            System.out.println(Idioma.t("aprovacao.nada.pendente"));
            listarPendentesDeOutros(logado);
            return;
        }

        Console.linha();
        System.out.printf("%-10s %-10s %-12s %14s %-14s %s%n", Idioma.t("aprovacao.col.id"),
                Idioma.t("aprovacao.col.projeto"), Idioma.t("aprovacao.col.tipo"),
                Idioma.t("aprovacao.col.valor"), Idioma.t("aprovacao.col.solicitante"),
                Idioma.t("aprovacao.col.descricao"));
        Console.linha();
        for (Aprovacao a : minhas) {
            System.out.printf("%-10s %-10s %-12s %14s %-14s %s%n", a.getId(), a.getProjetoId(),
                    rotuloTipo(a.getTipo()), Formato.moeda(a.getValor()),
                    a.getSolicitante(), a.getDescricao());
        }
        Console.linha();

        String id = Console.lerTexto(scanner, Idioma.t("aprovacao.id"));
        Aprovacao escolhida = buscarAprovacao(id);
        Projeto projeto = (escolhida == null) ? null : buscarProjeto(escolhida.getProjetoId());

        Permissoes.Resultado pode = Permissoes.podeAprovar(logado, escolhida, projeto);
        if (!pode.permitido) {
            Console.aviso(Idioma.t(pode.motivo));
            LogAuditoria.registrar(usuarioAtual, "APROVACAO_NEGADA",
                    "ID=" + id + ", Motivo=" + pode.motivo);
            return;
        }

        String resposta = Console.lerTexto(scanner, Idioma.t("aprovacao.confirmar"));
        boolean aprovou = resposta.equalsIgnoreCase(Idioma.t("venda.letra.sim"));
        escolhida.decidir(aprovou ? Aprovacao.APROVADA : Aprovacao.REJEITADA,
                logado.getUsername());
        salvarAprovacoes();
        LogAuditoria.registrar(usuarioAtual, aprovou ? "APROVOU" : "REJEITOU",
                "ID=" + escolhida.getId() + ", Valor=" + escolhida.getValor());

        if (aprovou) {
            Console.ok(Idioma.t("aprovacao.aprovada", escolhida.getId()));
            avisarEstouroDeOrcamento(projeto);
        } else {
            Console.ok(Idioma.t("aprovacao.rejeitada", escolhida.getId()));
        }
    }

    private void listarPendentesDeOutros(Usuario logado) {
        for (Aprovacao a : aprovacoes) {
            if (!a.isPendente()) {
                continue;
            }
            Permissoes.Resultado motivo =
                    Permissoes.podeAprovar(logado, a, buscarProjeto(a.getProjetoId()));
            System.out.println("   " + a.getId() + " (" + a.getProjetoId() + ", "
                    + Formato.moeda(a.getValor()) + ") - " + Idioma.t(motivo.motivo));
        }
    }

    private void avisarEstouroDeOrcamento(Projeto projeto) {
        BigDecimal gasto = gastoDoProjeto(projeto.getId());
        if (gasto.compareTo(projeto.getOrcamento()) > 0) {
            Console.aviso(Idioma.t("projeto.orcamento.estourado",
                    Formato.moeda(gasto), Formato.moeda(projeto.getOrcamento())));
        }
    }

    public void relatorioDesempenho(Usuario logado) {
        Console.titulo(Idioma.t("projeto.relatorio.titulo"));
        if (projetos.isEmpty()) {
            System.out.println(Idioma.t("projeto.relatorio.vazio"));
            return;
        }

        Console.linha();
        System.out.printf("%-8s %-22s %-12s %13s %13s %10s %6s%n",
                Idioma.t("projeto.col.id"), Idioma.t("projeto.col.nome"),
                Idioma.t("projeto.col.lider"), Idioma.t("projeto.col.orcamento"),
                Idioma.t("projeto.col.gasto"), Idioma.t("projeto.col.consumo"),
                Idioma.t("projeto.col.pend"));
        Console.linha();

        int mostrados = 0;
        for (Projeto projeto : projetos) {
            if (!Permissoes.podeVerProjeto(logado, projeto).permitido) {
                continue;
            }
            mostrados++;

            BigDecimal gasto = gastoDoProjeto(projeto.getId());
            double consumo = projeto.getOrcamento().signum() == 0 ? 0
                    : gasto.doubleValue() * 100 / projeto.getOrcamento().doubleValue();
            int pendentes = 0;
            for (Aprovacao a : aprovacoesDoProjeto(projeto.getId())) {
                if (a.isPendente()) {
                    pendentes++;
                }
            }

            System.out.printf("%-8s %-22s %-12s %13s %13s %9s%% %6d%n",
                    projeto.getId(), cortar(projeto.getNome(), 22), cortar(projeto.getLider(), 12),
                    Formato.moeda(projeto.getOrcamento()), Formato.moeda(gasto),
                    Formato.numero(consumo, 1), pendentes);

            if (consumo > 100) {
                Console.aviso(Idioma.t("projeto.alerta.estouro", projeto.getId()));
            } else if (consumo > 80) {
                Console.aviso(Idioma.t("projeto.alerta.atencao", projeto.getId()));
            }
        }
        Console.linha();

        if (mostrados == 0) {
            System.out.println(Idioma.t("projeto.relatorio.nenhum.seu"));
        } else {
            System.out.println(Idioma.t("projeto.relatorio.rodape", mostrados, projetos.size()));
        }
        LogAuditoria.registrar(usuarioAtual, "RELATORIO_PROJETOS", "Projetos=" + mostrados);
    }

    public void relatorioHierarquia(Usuario logado) {
        Console.titulo(Idioma.t("hierarquia.titulo"));
        if (!Permissoes.podeVerHierarquia(logado)) {
            Console.aviso(Idioma.t("perm.so.admin.hierarquia"));
            return;
        }

        List<Usuario> usuarios = estoque.getUsuarios();
        List<Usuario> duplos = new ArrayList<>();
        for (Usuario u : usuarios) {
            if (u.temDuploReporte()) {
                duplos.add(u);
            }
        }

        Console.linha();
        System.out.printf("%-14s %-16s %-16s %-18s %s%n", Idioma.t("hierarquia.col.usuario"),
                Idioma.t("hierarquia.col.funcional"), Idioma.t("hierarquia.col.projeto"),
                Idioma.t("hierarquia.col.papel"), Idioma.t("hierarquia.col.destino"));
        Console.linha();
        for (Usuario u : duplos) {
            System.out.printf("%-14s %-16s %-16s %-18s %s%n", cortar(u.getUsername(), 14),
                    cortar(u.getGerenteFuncional(), 16), cortar(u.getGerenteProjeto(), 16),
                    cortar(Permissoes.rotulo(u.getRole()), 18), destinoNaNovaEstrutura(u));
        }
        Console.linha();

        System.out.printf("%-34s %d%n", Idioma.t("hierarquia.total"), usuarios.size());
        System.out.printf("%-34s %d%n", Idioma.t("hierarquia.duplos"), duplos.size());

        Map<String, Integer> porPapel = new LinkedHashMap<>();
        for (Usuario u : usuarios) {
            porPapel.merge(u.getRole(), 1, Integer::sum);
        }
        System.out.println();
        for (Map.Entry<String, Integer> e : porPapel.entrySet()) {
            System.out.printf("   %-20s %d%n", Permissoes.rotulo(e.getKey()), e.getValue());
        }

        System.out.println();
        if (duplos.isEmpty()) {
            Console.ok(Idioma.t("hierarquia.sem.duplos"));
        } else {
            Console.aviso(Idioma.t("hierarquia.acao", duplos.size()));
        }
        LogAuditoria.registrar(usuarioAtual, "RELATORIO_HIERARQUIA", "Duplos=" + duplos.size());
    }

    private String destinoNaNovaEstrutura(Usuario usuario) {
        for (Projeto projeto : projetos) {
            if (projeto.getLider().equals(usuario.getUsername())) {
                return Idioma.t("hierarquia.destino.lider", projeto.getId());
            }
        }
        for (Projeto projeto : projetos) {
            if (projeto.participa(usuario.getUsername())) {
                return Idioma.t("hierarquia.destino.membro", projeto.getLider());
            }
        }
        return Idioma.t("hierarquia.destino.sem.projeto");
    }

    public static String rotuloTipo(String tipo) {
        return Aprovacao.TIPO_COMPRA.equals(tipo)
                ? Idioma.t("aprovacao.tipo.compra") : Idioma.t("aprovacao.tipo.orcamento");
    }

    public static String rotuloStatus(String status) {
        return switch (status) {
            case Aprovacao.APROVADA -> Idioma.t("aprovacao.status.aprovada");
            case Aprovacao.REJEITADA -> Idioma.t("aprovacao.status.rejeitada");
            default -> Idioma.t("aprovacao.status.pendente");
        };
    }

    private static String cortar(String texto, int tamanho) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= tamanho ? texto : texto.substring(0, tamanho - 1) + "…";
    }

    private void carregarProjetos() throws IOException {
        File arquivo = new File(PROJETOS_ARQUIVO);
        if (!arquivo.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (!linha.isBlank() && !linha.startsWith("#")) {
                    projetos.add(Projeto.fromString(linha));
                }
            }
        }
    }

    private void carregarAprovacoes() throws IOException {
        File arquivo = new File(APROVACOES_ARQUIVO);
        if (!arquivo.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (!linha.isBlank() && !linha.startsWith("#")) {
                    aprovacoes.add(Aprovacao.fromString(linha));
                }
            }
        }
    }

    public void salvarProjetos() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PROJETOS_ARQUIVO))) {
            for (Projeto projeto : projetos) {
                writer.write(projeto.toString());
                writer.newLine();
            }
        }
    }

    public void salvarAprovacoes() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(APROVACOES_ARQUIVO))) {
            for (Aprovacao aprovacao : aprovacoes) {
                writer.write(aprovacao.toString());
                writer.newLine();
            }
        }
    }

    public void adicionarProjeto(Projeto projeto) throws IOException {
        projetos.add(projeto);
        salvarProjetos();
    }

    public Aprovacao adicionarAprovacao(String projetoId, String tipo, BigDecimal valor,
                                        String solicitante, String descricao) throws IOException {
        Aprovacao nova = new Aprovacao(UUID.randomUUID().toString().substring(0, 8),
                projetoId, tipo, valor, solicitante, Aprovacao.PENDENTE,
                Formato.agoraParaArquivo(), "", descricao);
        aprovacoes.add(nova);
        salvarAprovacoes();
        return nova;
    }
}
