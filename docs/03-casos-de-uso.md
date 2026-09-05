# Casos de Uso

## UC001 - Login

Ator:
Usuário

Fluxo:

1. Informar credenciais.
2. Validar acesso.
3. Redirecionar para dashboard.

---

## UC002 - Cadastrar Adolescente

Ator:
Administrador, Discipulador ou Co-líder

Fluxo:

1. Acessar cadastro.
2. Selecionar o discipulado (ADMIN/GERENTE usam busca tipável por nome do grupo ou do discipulador; líderes usam o próprio grupo).
3. Conferir o contexto de liderança exibido (discipulador, co-líderes e, quando aplicável, faixa etária).
4. Informar dados, categoria e ficha de família (RN048, RN051); telefone pode ser omitido quando marcado que não possui.
5. Salvar.

Fluxo alternativo:

- Se o líder informar outro discipulado, rejeitar com `403` sem persistir o adolescente ou vínculo.
- Sem discipulado selecionado (ADMIN/GERENTE), a tela mostra apenas o total de adolescentes ativos; o detalhe por categoria exige seleção.

---

## UC003 - Registrar Encontro

Ator:
Administrador, Discipulador ou Co-líder

Fluxo:

1. Selecionar data.
2. Informar situação.
3. Salvar.
4. Opcionalmente, informar ou editar a observação do encontro (até 500 caracteres) ao atualizar o registro.

Fluxo alternativo:

- O administrador ou o discipulador do próprio grupo pode selecionar “Não realizado”, sendo obrigatório informar uma justificativa.
- O administrador ou o discipulador responsável pode corrigir a justificativa; somente o administrador pode voltar o encontro para “Realizado”, desde que não haja chamada ou visitantes registrados. Encontros de fechamento automático revertidos pelo administrador preservam o aviso de que o líder não lançou no prazo (RN052).
- Somente o administrador pode excluir o encontro da data (chamada, visitantes e registro), liberando a data para novo lançamento (RN053). A exclusão não desfaz promoção de visitante a discípulo.
- Gerentes consultam as não realizações da própria gerência no painel, sem permissão de alteração.
- A observação é independente da justificativa: pode existir em encontros realizados ou não realizados e não substitui a justificativa obrigatória de não realização.

---

## UC004 - Registrar Frequência

Ator:
Discipulador, Co-líder ou Administrador

Fluxo:

1. Selecionar encontro.
2. Marcar presença dos discípulos.
3. Opcionalmente marcar presença de GOE e visitantes cadastrados, somente se comparecerem.
4. Salvar.

Fluxo alternativo:

- Após o fechamento automático do prazo, somente o administrador modifica a frequência (reverte para realizado e preenche a chamada), mantendo o aviso de não lançamento pelo líder (RN052).
- Somente o administrador pode excluir a frequência de uma data lançada por engano (RN053).
- GOE e visitantes não entram como falta quando não marcados (RN049). Em discipulado de formação a chamada é uma lista simples, sem essa subdivisão (RN054).

---

## UC005 - Consultar Dashboard

Ator:
Todos

Fluxo:

1. Selecionar período (máximo 24 meses nos painéis).
2. Visualizar métricas.

O acesso é cumulativo: `GERENTE + DISCIPULADOR` visualiza “Minha gerência” e “Meu discipulado”; `ADMIN + DISCIPULADOR` visualiza o painel administrativo (visão executiva) e “Meu discipulado”. Co-líder possui a mesma visão histórica do próprio grupo.

A visão executiva administrativa consome `GET /painel/admin` (gerências ativas, evolução, ranking e encontros não realizados no período). A visão de gerência consome `GET /painel/gerencia` e inclui o nome do discipulador em cada grupo.

---

## UC006 - Consultar e imprimir relatório de frequência

Ator:
Administrador, Gerente, Discipulador ou Co-líder

Fluxo:

1. Acessar “Relatórios”.
2. Escolher a aba “Frequência” (discipulados regulares) ou “Frequência em formação” (grupos de formação). Administradores e discipuladores veem as duas abas; gerentes e co-líderes consultam somente o relatório regular.
3. Selecionar uma data inicial e uma data final, iguais para um único dia ou separadas por no máximo 12 meses.
4. Consultar os encontros realizados dentro do escopo dos perfis acumulados do usuário e do tipo escolhido.
5. Visualizar uma página por registro, com frequência dos adolescentes (ou discípulos, na formação) quando houve discipulado, ou a justificativa quando não houve.
6. Acionar “Imprimir / salvar como PDF” e usar o diálogo nativo do navegador.

Fluxos alternativos:

- Usuário com perfil permitido, mas sem associação organizacional aplicável: retornar `404`.
- Escopo válido sem registro no período: retornar lista vazia e informar o estado na tela.
- Registros de não ocorrência do discipulado são exibidos com a justificativa, sem lista de frequência.

---

## UC007 - Consultar Meu Discipulado

Ator:
Discipulador ou Co-líder

Fluxo:

1. Acessar “Meu discipulado”.
2. Selecionar um período de até 24 meses.
3. Visualizar indicadores, visitantes, percentual de presença e evolução mensal do próprio grupo.

Fluxos alternativos:

- Sem associação de liderança, retornar `404`.
- Com mais de uma associação em dados legados, retornar `409`.

---

## UC008 - Transferir Adolescente

Ator:
Administrador ou Gerente (escopo da gerência)

Fluxo:

1. Selecionar um adolescente com vínculo ativo.
2. Informar o discipulado de destino e a data de início.
3. Encerrar o vínculo anterior e criar o novo, preservando o histórico.

Fluxo alternativo:

- Discipulador ou co-líder tenta transferir: retornar `403`; a ação não é exibida na interface desses perfis.
- Gerente tenta transferir para discipulado de outra gerência: retornar `403`.
