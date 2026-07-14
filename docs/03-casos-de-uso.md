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
2. Informar dados.
3. Para discipulador ou co-líder, usar obrigatoriamente o próprio discipulado.
4. Salvar.

Fluxo alternativo:

- Se o líder informar outro discipulado, rejeitar com `403` sem persistir o adolescente ou vínculo.

---

## UC003 - Registrar Encontro

Ator:
Discipulador

Fluxo:

1. Selecionar data.
2. Informar situação.
3. Salvar.

---

## UC004 - Registrar Frequência

Ator:
Discipulador

Fluxo:

1. Selecionar encontro.
2. Marcar presença.
3. Salvar.

---

## UC005 - Consultar Dashboard

Ator:
Todos

Fluxo:

1. Selecionar período.
2. Visualizar métricas.

O acesso é cumulativo: `GERENTE + DISCIPULADOR` visualiza “Minha gerência” e “Meu discipulado”; `ADMIN + DISCIPULADOR` visualiza o painel administrativo e “Meu discipulado”. Co-líder possui a mesma visão histórica do próprio grupo.

---

## UC006 - Exportar Relatório

Ator:
Administrador

Fluxo:

1. Selecionar relatório.
2. Selecionar período.
3. Exportar.

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
Administrador

Fluxo:

1. Selecionar um adolescente com vínculo ativo.
2. Informar o discipulado de destino e a data de início.
3. Encerrar o vínculo anterior e criar o novo, preservando o histórico.

Fluxo alternativo:

- Discipulador ou co-líder tenta transferir: retornar `403`; a ação não é exibida na interface desses perfis.
