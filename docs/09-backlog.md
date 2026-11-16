# Product Backlog

Legenda de status (verificado no código/UI): **feito**, **parcial**, **aberto**.

## Sprint 1

- Login — **feito**
- Recuperação de senha — **feito**
- OAuth Google — **parcial** (entidade/`OAuthIdentityService` + enum; sem rotas HTTP em `AuthController`; botões UI não montados)
- OAuth Microsoft — **parcial** (idem)

---

## Sprint 2

- Gestão de usuários — **feito**
- Gestão de gerências — **feito**
- Gestão de discipulados (regulares e formação) — **feito**

---

## Sprint 3

- Cadastro de adolescentes — **feito**
- Inativação (manual + job RN019) — **feito**
- Histórico de vínculos — **feito**
- Ficha de família + filtros (UC012) — **feito**
- Anonimização de dados pessoais (RN062 / UC013) — **feito**

---

## Sprint 4

- Registro de encontros — **feito**
- Registro de frequência (padrão e formação) — **feito**
- Auditoria — **feito**
- Chamada de liderança (RN055–RN058) — **feito**

---

## Sprint 5

- Dashboard líder (“Meu discipulado”) — **feito**
- Dashboard gerente (+ desempenho embutido) — **feito**
- Dashboard administrador + visão executiva — **feito**
- Desempenho dos discipulados (RN061) — **feito**

### Divergências / follow-ups de produto

- Transferência de adolescente: a UI **não** habilita `podeTransferir` para nenhum perfil. A API aceita `ADMIN`/`GERENTE`/`DISCIPULADOR`/`CO_LIDER` com `exigirAlteracao` em origem e destino (RN029). Decidir se a UI deve expor a ação para ADMIN/GERENTE ou se a API deve restringir líderes.
- Associação única de liderança (RN028) — **feito** no backend (`exigirLiderancaLivre`); a UI só mostra o erro da API quando há conflito legado.
- Escopo de adolescentes para discipulador/co-líder — **feito** (seletores e API).

---

## Sprint 6

- Relatórios PDF/impressão — **feito** (impressão nativa do navegador)
- Relatórios Excel — **feito** (frequência e chamada de liderança)

---

## Sprint 7

- Aniversariantes (módulo dedicado) — **aberto** (hoje só coluna de aniversário na listagem)
- Melhorias gerais — **aberto**
