# Regras de Negócio

## Estrutura

RN001 - Um adolescente pertence a apenas um discipulado por vez.

RN002 - O histórico permanece associado ao discipulado original.

RN003 - Um discipulado possui exatamente um discipulador ativo.

RN004 - Um discipulado pode possuir até dois co-líderes.

RN005 - Um discipulado possui apenas um gerente.

---

## Frequência

RN006 - A frequência é registrada por encontro.

RN007 - Um encontro pode ser realizado ou cancelado.

RN008 - O líder pode registrar presença.

RN009 - O co-líder pode registrar presença.

RN010 - A frequência pode ser alterada em até três horas.

RN011 - Após três horas somente administradores podem alterar.

RN012 - O sistema deve registrar auditoria das alterações.

---

## Usuários

RN013 - Administradores criam usuários.

RN014 - Administradores gerenciam permissões.

RN015 - Um usuário pode acumular papéis e recebe a união das visões e permissões correspondentes a cada papel.

Exemplo:

- Gerente
- Discipulador

---

## Adolescentes

RN016 - Discipuladores podem cadastrar, atualizar e inativar adolescentes somente no próprio discipulado.

RN017 - Co-líderes podem cadastrar, atualizar e inativar adolescentes somente no próprio discipulado.

RN018 - O histórico de adolescentes inativados deve ser preservado.

RN019 - Um adolescente torna-se inativado após três meses sem participação.

---

## Relatórios

RN020 - O sistema deve gerar relatórios em PDF.

RN021 - O sistema deve gerar relatórios em Excel.

RN022 - O sistema deve permitir impressão da chamada.

---

## Segurança

RN023 - Senhas nunca devem ser armazenadas em texto puro.

RN024 - Deve existir recuperação de senha.

RN025 - O sistema deve suportar OAuth2.

RN026 - O sistema deve suportar login Google.

RN027 - O sistema deve suportar login Microsoft.

---

## Liderança e indicadores

RN028 - Um usuário pode exercer a função de discipulador ou co-líder em apenas um discipulado no total, mesmo que acumule ambos os perfis.

RN029 - Somente administradores podem transferir adolescentes entre discipulados.

RN030 - Discipulador e co-líder podem consultar o histórico gráfico do próprio discipulado.

RN031 - O painel do discipulado considera somente o grupo no qual o usuário exerce liderança, independentemente de outros perfis acumulados.
