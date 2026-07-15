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

## Autenticação

Defina `ADMIN_INITIAL_EMAIL`, `JWT_SECRET`, `APP_PUBLIC_URL` e as variáveis `MAIL_*` no `.env` antes da primeira inicialização. A API cria o administrador somente se esse e-mail ainda não existir e envia um convite para que ele defina a própria senha.

- `POST /api/v1/autenticacao/login`: retorna access token e refresh token.
- `POST /api/v1/autenticacao/atualizar-token`: renova a sessão e invalida o refresh token anterior.
- `POST /api/v1/autenticacao/esqueci-a-senha` e `POST /api/v1/autenticacao/redefinir-senha`: fluxo público de recuperação.
- `GET /api/v1/autenticacao/eu`: retorna o usuário autenticado.
- `/api/v1/usuarios/**`: gestão de usuários, restrita ao perfil `ADMIN`; o cadastro envia um convite e não recebe senha inicial.

Os tokens são aleatórios, persistidos somente como hash e enviados por SMTP em links de uso único. Senhas são definidas pelo próprio usuário e armazenadas somente como hash BCrypt.

### E-mail de definição e recuperação

No ambiente Docker, o Mailpit recebe os e-mails em `http://localhost:8025`. Em produção, ative o perfil `prod` e configure `APP_PUBLIC_URL` com HTTPS, além de `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` e `MAIL_TLS=true`. A aplicação recusa iniciar nesse perfil quando a configuração segura está incompleta.

`PASSWORD_RESET_MINUTES` controla a validade do link de recuperação (30 minutos por padrão) e `PASSWORD_SETUP_HOURS` controla a validade do convite inicial (24 horas por padrão). O token bruto existe apenas durante o envio: o banco mantém somente seu hash e os logs não registram token nem destinatário.

Para acompanhar uma operação, use `docker compose logs -f backend`. Cada requisição de negócio recebe um `traceId`, devolvido também no header `X-Trace-Id`; os eventos de e-mail registram o mesmo identificador, o ID interno do usuário, o tipo do convite e o resultado de cada tentativa, sem registrar destinatário ou token.
