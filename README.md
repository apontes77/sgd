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

## Convenções iniciais

- Alterações no banco são feitas exclusivamente em `backend/src/main/resources/db/migration/`, em novas migrações Flyway.
- Endpoints da aplicação usam o prefixo `/api`; endpoints operacionais do Spring Actuator ficam em `/actuator`.
- Respostas de erro não expõem detalhes internos; exceções de domínio serão convertidas para o contrato de erro padrão.
- O pipeline valida backend (`mvn verify`) e frontend (lint e build) em push e pull request.
