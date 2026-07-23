# Arquitetura

## Frontend

- React 18
- TypeScript
- Vite
- Material UI
- ECharts
- ESLint + Prettier

## Backend

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate
- Spotless, PMD, SpotBugs e JaCoCo

## Banco

- PostgreSQL
- Flyway

## Relatórios

- HTML/CSS com impressão nativa do navegador (relatório diário)
- JasperReports e Excel permanecem no roadmap

## Infraestrutura

- Docker
- Docker Compose
- Nginx
- Let's Encrypt

## CI/CD

- GitHub
- GitHub Actions
- Husky + lint-staged + commitlint (pre-commit e commit-msg)

## Estrutura Backend

Pacote-por-domínio em `br.com.sgd`:

- `adolescente`
- `audit`
- `auth`
- `common` (contratos compartilhados, como `PaginaResponse`)
- `config`
- `exception`
- `frequencia`
- `health`
- `observability`
- `organizacao`
- `painel`
- `relatorio`
- `user`

Cada pacote de domínio concentra controller, service, repository e entidades relacionadas.

## Estrutura Frontend

Organização por feature em `frontend/src`:

- `app/` — shell da aplicação (`App`, `AuthenticatedApp`, `theme`, `main`)
- `shared/` — HTTP client, tipos compartilhados e componentes de UI
- `features/` — módulos de domínio:
  - `auth`
  - `users`
  - `organizacao`
  - `adolescentes`
  - `frequencia`
  - `dashboards`
  - `relatorios`
- `test/` — setup do Vitest

Imports entre pastas usam o alias `@/*`.
