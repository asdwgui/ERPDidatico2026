package com.erp;

import java.math.BigDecimal;

public class Permissoes {

    public static final String ADMIN = "Admin";
    public static final String OPERADOR = "Operador";
    public static final String LIDER = "LiderProjeto";
    public static final String MEMBRO = "MembroProjeto";

    public static final BigDecimal TETO_DO_LIDER = new BigDecimal("50000.00");

    public static class Resultado {
        public final boolean permitido;
        public final String motivo; // chave de idioma, vazia quando permitido

        private Resultado(boolean permitido, String motivo) {
            this.permitido = permitido;
            this.motivo = motivo;
        }

        public static Resultado sim() {
            return new Resultado(true, "");
        }

        public static Resultado nao(String chaveDoMotivo) {
            return new Resultado(false, chaveDoMotivo);
        }
    }

    public static boolean podeGerenciarProjetos(Usuario usuario) {
        return ADMIN.equals(usuario.getRole());
    }

    public static Resultado podeGerenciarEquipe(Usuario usuario, Projeto projeto) {
        if (ADMIN.equals(usuario.getRole())) {
            return Resultado.sim();
        }
        if (projeto == null) {
            return Resultado.nao("perm.projeto.inexistente");
        }
        if (!projeto.getLider().equals(usuario.getUsername())) {
            return Resultado.nao("perm.nao.e.lider");
        }
        return Resultado.sim();
    }

    public static Resultado podeSolicitar(Usuario usuario, Projeto projeto) {
        if (projeto == null) {
            return Resultado.nao("perm.projeto.inexistente");
        }
        if (!projeto.isAtivo()) {
            return Resultado.nao("perm.projeto.encerrado");
        }
        if (ADMIN.equals(usuario.getRole())) {
            return Resultado.sim();
        }
        if (!projeto.participa(usuario.getUsername())) {
            return Resultado.nao("perm.fora.do.projeto");
        }
        return Resultado.sim();
    }

    public static Resultado podeAprovar(Usuario usuario, Aprovacao aprovacao, Projeto projeto) {
        if (aprovacao == null) {
            return Resultado.nao("perm.solicitacao.inexistente");
        }
        if (!aprovacao.isPendente()) {
            return Resultado.nao("perm.ja.decidida");
        }

        if (aprovacao.getSolicitante().equals(usuario.getUsername())) {
            return Resultado.nao("perm.proprio.pedido");
        }

        if (ADMIN.equals(usuario.getRole())) {
            return Resultado.sim();
        }

        if (projeto == null) {
            return Resultado.nao("perm.projeto.inexistente");
        }

        if (!projeto.getLider().equals(usuario.getUsername())) {
            return Resultado.nao("perm.nao.e.lider");
        }

        if (aprovacao.getValor().compareTo(TETO_DO_LIDER) > 0) {
            return Resultado.nao("perm.acima.do.teto");
        }

        return Resultado.sim();
    }

    public static Resultado podeVerProjeto(Usuario usuario, Projeto projeto) {
        if (ADMIN.equals(usuario.getRole())) {
            return Resultado.sim();
        }
        if (projeto == null) {
            return Resultado.nao("perm.projeto.inexistente");
        }
        if (!projeto.participa(usuario.getUsername())) {
            return Resultado.nao("perm.fora.do.projeto");
        }
        return Resultado.sim();
    }

    public static boolean podeVerHierarquia(Usuario usuario) {
        return ADMIN.equals(usuario.getRole());
    }

    public static boolean papelValido(String papel) {
        return ADMIN.equals(papel) || OPERADOR.equals(papel)
                || LIDER.equals(papel) || MEMBRO.equals(papel);
    }

    public static String rotulo(String papel) {
        return switch (papel) {
            case ADMIN -> Idioma.t("papel.admin");
            case OPERADOR -> Idioma.t("papel.operador");
            case LIDER -> Idioma.t("papel.lider");
            case MEMBRO -> Idioma.t("papel.membro");
            default -> papel;
        };
    }
}
