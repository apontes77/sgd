# SGD — Sistema de Gerenciamento de Discipulados

Monorepo do SGD: API Java/Spring, frontend React e documentação em `docs/`.

## Estrutura

- `backend/`: API Java 21 (Spring Boot, Flyway, JPA, Security/JWT).
- `frontend/`: React + TypeScript + Vite + Material UI.
- `docs/`: regras de negócio, modelo, OpenAPI e operação.
- `scripts/`: seed de testes e carga de estrutura organizacional.
- `compose.yaml`: PostgreSQL + API + interface em ambiente local.

## Executar com Docker

1. Copie `.env.example` para `.env` e altere `POSTGRES_PASSWORD`, `JWT_SECRET` e as credenciais do admin inicial.
2. Execute `docker compose up --build` na raiz do repositório.
3. Interface: `http://localhost:5173`. Saúde da API: `http://localhost:8080/api/health`.

## Executar localmente

PostgreSQL deve estar disponível com as variáveis de `.env`. São necessários Java 21, Maven 3.9+ e Node.js 22+.

```bash
# terminal 1
cd backend
mvn spring-boot:run

# terminal 2
cd frontend
corepack enable
pnpm install
pnpm run dev
```

Contrato da API (prefixo de negócio): `http://localhost:8080/api/v1`. Actuator em `/actuator`.

## Deploy em produção

Deploy recomendado no Render (frontend estático, API Docker, Postgres gerenciado). Consulte [Deploy no Render](docs/deploy-render.md) e [Observabilidade](docs/observability.md).

## Autenticação

Defina `ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD` e `JWT_SECRET` no `.env` antes da primeira inicialização. A API cria o administrador somente se esse e-mail ainda não existir.

Rotas (todas sob `/api/v1`):

- `POST /autenticacao/login` — access + refresh token
- `POST /autenticacao/atualizar-token` — renova a sessão e invalida o refresh anterior
- `POST /autenticacao/esqueci-a-senha` e `POST /autenticacao/redefinir-senha`
- `GET /autenticacao/eu` — usuário autenticado
- `/usuarios/**` — gestão de usuários (`ADMIN`)

Tokens de redefinição não são expostos pela API; em produção a entrega depende de provedor de e-mail transacional.

### Dados de teste

```bash
python3 scripts/seed_test_data.py
```

O script lê `ADMIN_INITIAL_EMAIL` / `ADMIN_INITIAL_PASSWORD` (ou `SGD_ADMIN_EMAIL` / `SGD_ADMIN_PASSWORD`). Destinos remotos exigem HTTPS e `--allow-remote`. Ver também [carga de estrutura](scripts/data/README.md).

## Convenções

- Migrações de banco somente em `backend/src/main/resources/db/migration/` (Flyway).
- Endpoints de negócio: `/api/v1/...`. Operacionais Spring: `/actuator`.
- Erros usam `application/problem+json` sem detalhes internos de stack.
- CI valida backend (`mvn verify`) e frontend (lint/test/build) em push e PR.
- Contrato executável: [docs/08-api.yaml](docs/08-api.yaml). Resumo: [docs/08-api.md](docs/08-api.md).

## Armadilhas comuns (desenvolvimento)

| Sintoma | Causa provável | O que fazer |
| --- | --- | --- |
| `403` ao lançar frequência na segunda | Prazo sexta→domingo 23:59 (`America/Sao_Paulo`) | Use admin ou ajuste o relógio/data do encontro nos testes |
| `403` após editar chamada | Janela de 3h a partir de `chamadaSalvaEm` | Somente `ADMIN` altera depois da janela |
| GOE não aparece na chamada | `DISCIPULO_GOE` é excluído da lista atual | Esperado (RN049); histórico antigo continua editável |
| Cadastro rejeita sem telefone/contato | Flags `naoPossuiTelefone` / `naoPossuiContatoFamiliar` | Enviar as flags explicitamente (RN048) |
| Painel do gerente vazio / `404` | Gerência inativa ou mais de uma ativa | Ative exatamente uma gerência para o usuário |
| Seed/carga remota recusada | Falta `--allow-remote` ou URL sem HTTPS | Siga `scripts/data/README.md` |

Documentação funcional completa em `docs/` (visão, glossário, regras, casos de uso, modelo e arquitetura).
