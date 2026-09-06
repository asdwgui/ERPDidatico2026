# ERP Didático 2026

Projeto da disciplina de Administração — implementação de um ERP simples em Java, a partir de uma base fornecida pelo professor, evoluído conforme os desafios propostos ao longo do curso.

## Desafio 1 — Adequação às Leis de Proteção de Dados (LGPD)

**Contexto:** o governo aprovou uma nova legislação de proteção de dados que afeta diretamente a forma como sistemas ERP devem ser desenvolvidos e operados. O objetivo deste desafio foi adaptar o ERP base para refletir, em nível didático, requisitos centrais da LGPD.

### Mudanças implementadas

| # | Mudança | Requisito da LGPD atendido |
|---|---|---|
| 1 | Base Legal na Pessoa | Vincular cada cadastro a uma base legal (Consentimento, Execução de Contrato, Obrigação Legal) |
| 2 | Log de Auditoria | Registro de quem acessou/alterou dados, com data e hora |
| 3 | Anonimização de Dados | Rotina para anonimizar dados de uma pessoa (ex: fim do período de retenção) |
| 4 | Controle de Acesso (RBAC) | Restrição de ações sensíveis por papel de usuário (Admin / Operador), pelo princípio do menor privilégio |

### Como rodar

1. Abra o projeto no IntelliJ IDEA (ou outra IDE com suporte a Java 17).
2. Execute a classe `Main.java`.
3. Faça login com um usuário e senha cadastrados em `usuarios.txt` (ex: `admin`).
4. Navegue pelo menu — as opções marcadas como **[Acesso Restrito]** só funcionam para o papel `Admin`.

### Testando o log de auditoria

Após usar o sistema, um arquivo `auditoria.log` é criado na raiz do projeto, com uma linha por ação relevante:

```
[2026-09-04 18:47:58] Usuário=admin - LOGIN - Papel=Admin
[2026-09-04 18:48:21] Usuário=admin - CADASTRO_PRODUTO - ID=7, Nome=controle
[2026-09-04 18:47:24] Usuário=operador - ACESSO_NEGADO - Tentou listar pessoas
```

## Limitações conhecidas (escopo didático)

Este projeto demonstra, em nível de sala de aula, como requisitos da LGPD se traduzem em decisões técnicas — mas **não configura, isoladamente, conformidade legal plena**. Ficam fora do escopo:

- **Hash de senha não criptográfico:** o método `Usuario.hash()` usa `String.hashCode()`, que não é seguro (colisões previsíveis, sem salt). Em um sistema real, usaríamos algo como BCrypt ou PBKDF2.
- **Sem criptografia** dos dados em trânsito ou em repouso.
- **Sem canal formal de contato do DPO (Encarregado)** nem processo de resposta a incidentes junto à ANPD.
- **Sem persistência segura de credenciais** (login simplificado, sem sessão, sem expiração).
- Os arquivos de dados (`pessoas.txt`, `usuarios.txt`, etc.) não são versionados no Git (ver `.gitignore`) exatamente para evitar expor dados pessoais, mesmo que fictícios, em um repositório público.

## Estrutura do projeto

```
src/com/erp/
├── Main.java          # Menu principal, login e controle de acesso (RBAC)
├── Estoque.java        # Regras de negócio: produtos, pessoas, títulos
├── Pessoa.java          # Cadastro de pessoas + base legal (LGPD)
├── Usuario.java         # Autenticação e papel do usuário
├── LogAuditoria.java     # Registro de auditoria (quem, o quê, quando)
└── Titulo.java          # Títulos financeiros (compra/venda/pagamento)
```

## Autores

Grupo — Disciplina de Administração, 2026.
