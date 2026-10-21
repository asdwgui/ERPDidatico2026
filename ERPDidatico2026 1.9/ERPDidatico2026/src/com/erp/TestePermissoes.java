package com.erp;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TestePermissoes {

    private static int passou = 0;
    private static int falhou = 0;

    public static void main(String[] args) throws IOException {
        Console.linha();
        System.out.println("TESTE DE PERMISSÕES - ESTRUTURA PROJETIZADA");
        Console.linha();

        Usuario admin = new Usuario("ana", "x", Permissoes.ADMIN, "pt");
        Usuario liderAlfa = new Usuario("bruno", "x", Permissoes.LIDER, "pt");
        Usuario liderBeta = new Usuario("carla", "x", Permissoes.LIDER, "pt");
        Usuario membro = new Usuario("davi", "x", Permissoes.MEMBRO, "pt");
        Usuario deFora = new Usuario("elisa", "x", Permissoes.OPERADOR, "pt");

        List<String> equipeAlfa = new ArrayList<>();
        equipeAlfa.add("davi");
        Projeto alfa = new Projeto("ALFA", "Expansão Alemanha", "bruno",
                new BigDecimal("100000.00"), Projeto.ATIVO, equipeAlfa);
        Projeto beta = new Projeto("BETA", "Troca de servidor", "carla",
                new BigDecimal("40000.00"), Projeto.ATIVO, new ArrayList<>());
        Projeto encerrado = new Projeto("GAMA", "Projeto antigo", "bruno",
                new BigDecimal("10000.00"), Projeto.ENCERRADO, new ArrayList<>());

        Aprovacao pedidoDoMembro = pedido("A1", "ALFA", "davi", "3000.00");
        Aprovacao pedidoDoLider = pedido("A2", "ALFA", "bruno", "2000.00");
        Aprovacao pedidoCaro = pedido("A3", "ALFA", "davi", "80000.00");
        Aprovacao pedidoDoBeta = pedido("A4", "BETA", "carla", "1000.00");
        Aprovacao jaDecidida = pedido("A5", "ALFA", "davi", "500.00");
        jaDecidida.decidir(Aprovacao.APROVADA, "bruno");

        titulo("QUEM PODE SOLICITAR");
        conferir("membro solicita no projeto dele", true,
                Permissoes.podeSolicitar(membro, alfa));
        conferir("líder solicita no projeto dele", true,
                Permissoes.podeSolicitar(liderAlfa, alfa));
        conferir("admin solicita em qualquer projeto", true,
                Permissoes.podeSolicitar(admin, beta));
        conferir("membro do Alfa solicita no Beta", false,
                Permissoes.podeSolicitar(membro, beta));
        conferir("gente de fora solicita", false,
                Permissoes.podeSolicitar(deFora, alfa));
        conferir("solicitar em projeto encerrado", false,
                Permissoes.podeSolicitar(liderAlfa, encerrado));

        titulo("QUEM PODE APROVAR  (as tres travas)");
        conferir("TRAVA 1 - líder aprova pedido do membro dele", true,
                Permissoes.podeAprovar(liderAlfa, pedidoDoMembro, alfa));
        conferir("TRAVA 1 - líder do Beta aprova pedido do Alfa", false,
                Permissoes.podeAprovar(liderBeta, pedidoDoMembro, alfa));
        conferir("TRAVA 1 - membro aprova pedido de colega", false,
                Permissoes.podeAprovar(membro, pedidoDoLider, alfa));

        conferir("TRAVA 2 - líder aprova o PRÓPRIO pedido", false,
                Permissoes.podeAprovar(liderAlfa, pedidoDoLider, alfa));
        conferir("TRAVA 2 - membro aprova o PRÓPRIO pedido", false,
                Permissoes.podeAprovar(membro, pedidoDoMembro, alfa));
        conferir("TRAVA 2 - admin aprova o PRÓPRIO pedido", false,
                Permissoes.podeAprovar(admin, pedido("A6", "ALFA", "ana", "100.00"), alfa));

        conferir("TRAVA 3 - líder aprova R$ 80.000 (acima do teto)", false,
                Permissoes.podeAprovar(liderAlfa, pedidoCaro, alfa));
        conferir("TRAVA 3 - admin aprova R$ 80.000", true,
                Permissoes.podeAprovar(admin, pedidoCaro, alfa));

        conferir("admin aprova pedido de qualquer projeto", true,
                Permissoes.podeAprovar(admin, pedidoDoBeta, beta));
        conferir("aprovar solicitação já decidida", false,
                Permissoes.podeAprovar(liderAlfa, jaDecidida, alfa));

        titulo("EQUIPE E RELATÓRIOS");
        conferir("líder mexe na equipe do projeto dele", true,
                Permissoes.podeGerenciarEquipe(liderAlfa, alfa));
        conferir("líder mexe na equipe do projeto do vizinho", false,
                Permissoes.podeGerenciarEquipe(liderAlfa, beta));
        conferir("membro mexe na equipe", false,
                Permissoes.podeGerenciarEquipe(membro, alfa));
        conferir("membro vê o relatório do projeto dele", true,
                Permissoes.podeVerProjeto(membro, alfa));
        conferir("membro vê o relatório de outro projeto", false,
                Permissoes.podeVerProjeto(membro, beta));
        conferir("admin vê qualquer projeto", true,
                Permissoes.podeVerProjeto(admin, beta));

        titulo("ESTRUTURA DA EMPRESA (continua com o Admin)");
        conferirSimples("admin cria projeto", true, Permissoes.podeGerenciarProjetos(admin));
        conferirSimples("líder cria projeto", false, Permissoes.podeGerenciarProjetos(liderAlfa));
        conferirSimples("admin vê o organograma", true, Permissoes.podeVerHierarquia(admin));
        conferirSimples("líder vê o organograma", false, Permissoes.podeVerHierarquia(liderAlfa));

        Console.linha();
        System.out.printf("%d conferências, %d de acordo, %d FORA DO ESPERADO%n",
                passou + falhou, passou, falhou);
        if (falhou == 0) {
            System.out.println("Mapa de permissões íntegro: a estrutura projetizada está");
            System.out.println("configurada e a segregação de funções continua valendo.");
        } else {
            System.out.println("ATENÇÃO: alguma regra foi afrouxada. Não libere a transição.");
        }
        Console.linha();
    }

    private static Aprovacao pedido(String id, String projeto, String quem, String valor) {
        return new Aprovacao(id, projeto, Aprovacao.TIPO_COMPRA, new BigDecimal(valor),
                quem, Aprovacao.PENDENTE, "2026-10-20 09:00", "", "teste");
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println(">>> " + texto);
        Console.linha();
    }

    private static void conferir(String caso, boolean esperado, Permissoes.Resultado obtido) {
        registrar(caso, esperado, obtido.permitido,
                obtido.permitido ? "" : Idioma.t(obtido.motivo));
    }

    private static void conferirSimples(String caso, boolean esperado, boolean obtido) {
        registrar(caso, esperado, obtido, "");
    }

    private static void registrar(String caso, boolean esperado, boolean obtido, String motivo) {
        boolean bateu = (esperado == obtido);
        if (bateu) {
            passou++;
        } else {
            falhou++;
        }
        System.out.printf("  %-5s %-48s %-8s %s%n",
                bateu ? "[ok]" : "[!!]", caso,
                obtido ? "permite" : "recusa", motivo);
    }
}
