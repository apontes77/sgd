# SGD — Sistema de Gerenciamento de Discipulados

Base do monorepo do SGD. A camada de domínio ainda não foi implementada; esta entrega prepara a execução local, o banco e a automação de qualidade.

## Estrutura

- `backend/`: API Java 21 com Spring Boot, Flyway, JPA e endpoint de saúde.
- `frontend/`: aplicação React com TypeScript, Vite e Material UI.
- `docs/`: documentação funcional e modelo de dados.
- `compose.yaml`: ambiente local integrado com PostgreSQL, API e interface.

## Executar com Docker

1. Copie `.env.example` para `.env` e altere `POSTGRES_PASSWORD`.
2. Execute `docker compose up --build` na raiz do repositório.
3. Acesse a interface em `http://localhost:5173` e a saúde da API em `http://localhost:8080/api/health`.

## Executar localmente

O PostgreSQL deve estar disponível com as variáveis definidas em `.env` ou no ambiente. São necessários Java 21, Maven 3.9+ e Node.js 22+.

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

## Deploy em produção

O deploy recomendado usa Render para o frontend estático, a API Docker e o PostgreSQL gerenciado. Consulte [Deploy no Render](docs/deploy-render.md) para provisionamento e operação e [Observabilidade](docs/observability.md) para OTLP, dashboards, alertas e proteção de dados.

## Convenções iniciais

- Alterações no banco são feitas exclusivamente em `backend/src/main/resources/db/migration/`, em novas migrações Flyway.
- Endpoints da aplicação usam o prefixo `/api`; endpoints operacionais do Spring Actuator ficam em `/actuator`.
- Respostas de erro não expõem detalhes internos; exceções de domínio serão convertidas para o contrato de erro padrão.
- O pipeline valida backend (`mvn verify`) e frontend (lint e build) em push e pull request.

## Autenticação

Defina `ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD` e `JWT_SECRET` no `.env` antes da primeira inicialização. A API cria o administrador somente se esse e-mail ainda não existir.

- `POST /api/auth/login`: retorna access token e refresh token.
- `POST /api/auth/refresh`: renova a sessão e invalida o refresh token anterior.
- `POST /api/auth/forgot-password` e `POST /api/auth/reset-password`: fluxo de recuperação de senha.
- `GET /api/auth/me`: retorna o usuário autenticado.
- `/api/users/**`: gestão de usuários, restrita ao perfil `ADMIN`.

Os tokens de redefinição não são expostos pela API. A entrega por e-mail deve ser conectada a um provedor transacional antes da publicação em produção.

### Dados de teste para desenvolvimento e homologação

Com a aplicação em execução, o script abaixo cria ou atualiza os usuários e a estrutura organizacional usados nos testes manuais:

```bash
python3 scripts/seed_test_data.py
```

O script lê as credenciais do administrador inicial em `ADMIN_INITIAL_EMAIL` e `ADMIN_INITIAL_PASSWORD` no `.env`. Como alternativa, aceita `SGD_ADMIN_EMAIL` e `SGD_ADMIN_PASSWORD` no ambiente.

Para executar contra o ambiente remoto de homologação, informe as credenciais por variáveis de ambiente e confirme explicitamente o destino remoto:

```bash
SGD_ADMIN_EMAIL='admin@exemplo.com' \
SGD_ADMIN_PASSWORD='senha-do-admin' \
python3 scripts/seed_test_data.py \
  --api-url 'https://api-homologacao.exemplo.com/api/v1' \
  --allow-remote
```

Destinos remotos exigem HTTPS. O script não é executado automaticamente no deploy: ele deve ser acionado de forma consciente depois que a API estiver disponível.
