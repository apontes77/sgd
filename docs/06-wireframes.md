# Wireframes

Navegação autenticada: rotas `/app/{seção}` (`frontend/src/app/appNavigation.ts`). Papéis acumulados recebem a união dos itens. Grupos do menu: **Dashboards & BI → Cadastros → Operações → Relatórios**.

## Mapa de navegação (UI atual)

| Seção | Rota | Quem vê |
| --- | --- | --- |
| Visão executiva | `/app/visao-executiva` | ADMIN, GERENTE |
| Painel | `/app/painel` | ADMIN |
| Minha gerência | `/app/minha-gerencia` | GERENTE |
| Meu discipulado | `/app/meu-discipulado` | DISCIPULADOR, CO_LIDER |
| Estrutura | `/app/estrutura` | ADMIN |
| Usuários | `/app/usuarios` | ADMIN |
| Adolescentes | `/app/adolescentes` | todos (`?discipuladoId=` opcional) |
| Famílias | `/app/familias` | todos |
| Encontros e frequência / Registrar frequência | `/app/frequencia` | ADMIN, DISCIPULADOR, CO_LIDER |
| Frequência em formação | `/app/frequencia-formacao` | ADMIN; DISCIPULADOR se lidera ≥1 grupo `emFormacao` |
| Chamada de liderança | `/app/chamada-lideranca` | ADMIN |
| Relatórios | `/app/relatorios` | todos (abas internas variam por perfil) |

Público (sem sessão): `/` (login e-mail/senha), `/esqueci-senha`, `/redefinir-senha`. Botões OAuth existem no código (`OAuthLoginButtons`) mas **não** estão montados no login (RN025–RN027).

---

## Meu discipulado

+---------------------+
| Período: início/fim |
+---------------------+

+---------------------+
| KPIs                 |
| Encontros            |
| Presentes/Ausentes   |
| Visitantes/Presença  |
+---------------------+

+---------------------+
| Evolução Mensal     |
+---------------------+

+---------------------+
| Tabela acessível    |
| do histórico        |
+---------------------+

Discipulador e co-líder acessam a mesma visão do próprio grupo. Usuários com papéis acumulados mantêm também seus demais painéis: gerente vê “Minha gerência” e administrador vê “Painel”.

Na área de adolescentes, discipulador e co-líder visualizam somente o próprio grupo nos seletores. A ação **Transferir** não é exposta na UI para nenhum perfil hoje (`podeTransferir` permanece desligado em `AuthenticatedApp`). A ficha de família (menu Famílias e cadastro do adolescente) está disponível para todos os perfis no respectivo escopo. ADMIN vê **Excluir dados** (anonimização) enquanto `anonimizado` for falso.

---

## Dashboard Gerente

+---------------------+
| Média da Gerência   |
+---------------------+

+---------------------+
| Discipulados        |
+---------------------+

+---------------------+
| Evolução Trimestral |
+---------------------+

+---------------------+
| Desempenho dos      |
| discipulados        |
| (embutido)          |
+---------------------+

Abaixo do painel da gerência, a UI embute o mesmo componente de desempenho usado em Estrutura (RN061).

---

## Dashboard Admin

+---------------------+
| Total Igreja        |
+---------------------+

+---------------------+
| Por Gerência        |
+---------------------+

+---------------------+
| Masculino/Feminino  |
+---------------------+

---

## Visão executiva

ADMIN e GERENTE. KPIs consolidados com atalho para o detalhe (`Painel` ou `Minha gerência`).

---

## Estrutura (ADMIN)

Abas:

1. Gerências
2. Discipulados (regulares)
3. Discipulados de formação (sem gerência/co-líder no formulário)
4. Desempenho dos discipulados — período, recortes (discipulado/sexo/faixa/gerência), gráficos e tabelas (RN061)

---

## Operações e cadastros (resumo)

- **Frequência**: encontros e chamada de adolescentes; em formação usa a mesma tela com `emFormacao`.
- **Chamada de liderança**: grade ADMIN por data; formação fora da lista (RN055).
- **Famílias**: listagem filtrável (UC012) e edição da ficha no escopo.
- **Relatórios**: abas Frequência, Formação (quando elegível), Adolescentes (ADMIN) e Liderança (ADMIN).
