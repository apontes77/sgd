# Testes E2E (Playwright)

Smoke tests de browser contra a aplicação real (frontend + API).

## Pré-requisitos

1. API em `http://localhost:8080` (ex.: `cd backend && SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`).
2. Credenciais do administrador inicial disponíveis no ambiente (`.env` da raiz ou variáveis `E2E_ADMIN_*`).
3. Dependências do frontend instaladas (`pnpm install`).
4. Browser Chromium do Playwright: `pnpm exec playwright install chromium`.

## Executar

```bash
# na pasta frontend — sobe o Vite (dev:e2e) automaticamente e usa o proxy /api → :8080
pnpm run test:e2e
```

Variáveis úteis:

| Variável | Descrição |
|---|---|
| `E2E_ADMIN_EMAIL` / `E2E_ADMIN_PASSWORD` | Credenciais do admin (fallback: `ADMIN_INITIAL_*`) |
| `E2E_BASE_URL` | URL do frontend (padrão `http://127.0.0.1:5173`) |
| `E2E_START_WEBSERVER=false` | Não iniciar o Vite (já está rodando) |

## CI

O workflow GitHub Actions sobe PostgreSQL, a API com perfil `local` e executa estes testes no job `e2e`.
