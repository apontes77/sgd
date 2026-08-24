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

RN007 - Um encontro pode ser realizado ou não realizado.

RN008 - O líder pode registrar presença.

RN009 - O co-líder pode registrar presença.

RN010 - Após o primeiro salvamento da chamada de um encontro realizado, a frequência pode ser alterada em até três horas a partir desse instante.

RN011 - Após três horas do primeiro salvamento da chamada, somente administradores podem alterar a frequência.

RN012 - O sistema deve registrar auditoria das alterações.

RN046 - Encontros com data de sexta-feira podem ser lançados (criação do encontro, primeiro salvamento da chamada ou marcação de não realizado) até o domingo subsequente às 23:59:59 no fuso `America/Sao_Paulo`. Após esse prazo, discipulador e co-líder não podem mais lançar aquela sexta; administradores podem.

RN047 - Se, após o domingo subsequente, um discipulado ativo não tiver chamada salva nem encontro não realizado para aquela sexta, o sistema registra automaticamente o encontro como `NAO_REALIZADO` com a justificativa `discipulador ou colider não registraram a frequência` e marca `fechamento_automatico`. Encontros `REALIZADO` sem chamada salva são convertidos para essa situação.

RN052 - Administradores podem reverter um encontro de fechamento automático (`fechamento_automatico`) de `NAO_REALIZADO` para `REALIZADO` e lançar ou alterar a chamada a qualquer momento. O flag permanece verdadeiro após a correção, para que o sistema continue informando que o discipulador/co-líder não lançou a frequência no prazo.

RN053 - Somente administradores podem excluir um encontro (chamada, visitantes e o registro da data). A exclusão é auditada, libera o par discipulado+data para novo lançamento e não desfaz promoção automática de categoria (RN045).

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

RN050 - Gerentes podem cadastrar, atualizar, inativar e transferir adolescentes somente nos discipulados da própria gerência. O ADMIN mantém superacesso global.

RN018 - O histórico de adolescentes inativados deve ser preservado.

RN019 - Um adolescente torna-se inativado após três meses sem participação.

RN041 - No cadastro, o adolescente recebe obrigatoriamente uma categoria: `DISCIPULO`, `VISITANTE` ou `DISCIPULO_GOE`. A categoria é independente da flag de ativo/inativo.

RN042 - A categoria `DISCIPULO_GOE` exige motivo do afastamento. Demais categorias não persistem motivo.

RN048 - Cada adolescente possui exatamente uma ficha de família obrigatória (1:1). Contatos familiares (responsáveis, telefones, endereço e demais dados pastorais da ficha) vivem na ficha; campos sem informação usam o texto “Não consta”. Na categoria `DISCIPULO_GOE`, o telefone do próprio adolescente continua obrigatório no cadastro do adolescente, salvo quando marcado explicitamente que não possui telefone (`naoPossuiTelefone`).

RN051 - Leitura e escrita da ficha de família são exclusivas de ADMIN (qualquer registro) e GERENTE (somente na própria gerência). Discipulador e co-líder cadastram adolescentes sem informar a ficha; o sistema cria automaticamente a ficha com “Não consta”.

RN049 - A lista de presença (chamada) de um encontro inclui somente adolescentes ativos nas categorias `DISCIPULO` e `VISITANTE`. Discípulos `DISCIPULO_GOE` permanecem na base para registro histórico e na gestão de adolescentes, mas não entram como participantes atuais da frequência. Registros de frequência já existentes de um GOE em um encontro continuam visíveis/editáveis como registro anterior.

RN043 - Quando um adolescente ativo na categoria `DISCIPULO` acumula ao menos quatro faltas (`AUSENTE`) em encontros `REALIZADO` cuja data está nas últimas seis semanas (42 dias corridos, timezone America/Sao_Paulo), o sistema apresenta alerta de potencial Discípulo GOE. A mudança de categoria só ocorre mediante confirmação do usuário autorizado, com motivo informado.

RN044 - A listagem de adolescentes exibe três grupos (Discípulos, Visitantes e Discípulos GOE) por discipulado. Gerente e Administrador selecionam o discipulado; Discipulador e Co-líder visualizam o próprio grupo.

RN045 - Quando um adolescente ativo na categoria `VISITANTE` acumula ao menos três presenças (`PRESENTE`) em encontros `REALIZADO` cuja data está na janela de cinco semanas (35 dias corridos inclusivos) a partir do `dataInicio` do primeiro vínculo com discipulado, o sistema promove automaticamente a categoria para `DISCIPULO` e registra auditoria. A promoção ocorre ao salvar a chamada, sem confirmação do usuário, e é unidirecional (não rebaixa se a presença for editada depois). Ignora inativos, anonimizados e categorias `DISCIPULO`/`DISCIPULO_GOE`.

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

RN029 - Administradores e gerentes (somente dentro da própria gerência) podem transferir adolescentes entre discipulados. Discipulador e co-líder não transferem.

RN030 - Discipulador e co-líder podem consultar o histórico gráfico do próprio discipulado.

RN031 - O painel do discipulado considera somente o grupo no qual o usuário exerce liderança, independentemente de outros perfis acumulados.

---

## Relatório de frequência por período

RN032 - Administradores consultam todos os discipulados; gerentes consultam os discipulados das suas gerências ativas; discipuladores e co-líderes consultam somente os grupos em que exercem liderança.

RN033 - Usuários com perfis acumulados recebem a união dos escopos de relatório, e o perfil de administrador equivale ao acesso total.

RN034 - O relatório inclui somente encontros realizados e frequências efetivamente persistidas no período informado, preservando adolescentes transferidos ou inativados que constem na chamada histórica. O período pode representar um único dia e não pode exceder 12 meses.

RN035 - Cada encontro gera uma página independente, ordenada por data, gerência, discipulado e encontro, e pode ser impressa ou salva como PDF pelo diálogo nativo do navegador.

RN036 - Cada linha do relatório deve apresentar o nome e o telefone do adolescente, a data do encontro e a situação presente ou ausente.

---

## Encontros não realizados

RN037 - Administradores podem marcar um encontro como não realizado em qualquer discipulado ativo; o discipulador pode marcar essa situação e editar a justificativa somente no próprio discipulado. Apenas administradores podem reverter uma não realização para realizado. Gerentes e co-líderes não podem executar essas ações.

RN038 - Todo encontro não realizado deve possuir uma justificativa de até 500 caracteres, preservada no histórico e na auditoria.

RN039 - Gerentes consultam os encontros não realizados e suas justificativas somente nos discipulados da própria gerência; essas ocorrências não compõem indicadores nem o relatório diário de frequência. O painel administrativo (`GET /painel/admin`) e o painel da gerência (`GET /painel/gerencia`) listam apenas gerências ativas e expõem o nome do discipulador em cada indicador de discipulado do painel da gerência.

RN040 - Um discipulado possui no máximo um encontro por data, independentemente da situação. Não é possível registrar chamada ou visitantes em encontro não realizado.

RN050 - Um encontro pode armazenar uma observação opcional de até 500 caracteres (texto livre sobre o encontro, distinta da justificativa de não realização). A observação é criada/alterada via `PATCH /encontros/{id}` por quem já pode alterar o encontro; string vazia ou só espaços é persistida como nula. Alterações entram na auditoria do encontro.
