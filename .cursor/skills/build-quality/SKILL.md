---
name: build-quality
description: >-
  Executa build, lint, testes e quality gates do monorepo SGD (backend Maven
  Spotless/PMD/SpotBugs/JaCoCo e frontend ESLint/Prettier/Vitest/tsc). Use when
  the user asks to build, verify, validar qualidade, rodar lint, CI local,
  mvn verify, pnpm lint/test/build, coverage, ou checar se o código passa nos
  gates antes de commit/PR.
---

# Build & Quality (SGD)

Skill repetitiva para validar o monorepo exatamente como o CI e os plugins locais.

Sempre ler este arquivo e executar o fluxo abaixo — não inventar comandos alternativos.

## Escopos

| Pedido do usuário | Escopo |
|---|---|
| build / qualidade / CI local / validar tudo | `full` |
| só backend / Java / Maven | `backend` |
| só frontend / React / lint | `frontend` |
| espelhar GitHub Actions (inclui Docker + Render YAML) | `ci` |

Se o escopo não for claro, usar `full`.

## Pré-requisitos

- Raiz do repo: diretório que contém `backend/` e `frontend/`
- Backend: Java 21, Maven 3.9+
- Frontend: Node 22+, pnpm 11.7.0 (`corepack enable` se necessário)
- Escopo `ci`: Docker disponível

## Fluxo obrigatório

Copiar e atualizar:

```
Build Progress:
- [ ] 1. Escopo definido
- [ ] 2. Checks executados
- [ ] 3. Falhas corrigidas (se autodesejadas)
- [ ] 4. Re-execução até verde ou bloqueio reportado
- [ ] 5. Relatório final
```

### 1. Rodar o gate

Preferir o script da skill (espelha o CI):

```bash
bash .cursor/skills/build-quality/scripts/run-quality.sh <full|backend|frontend|ci>
```

Se o script não puder ser usado, executar os comandos equivalentes da seção [Comandos oficiais](#comandos-oficiais).

Usar `block_until_ms` alto o suficiente (`mvn clean verify` pode passar de alguns minutos). Não interromper cedo.

### 2. Em caso de falha

1. Ler o trecho relevante do log (plugin/fase/arquivo).
2. Corrigir a causa — não enfraquecer gates (thresholds, excludes, regras).
3. Autofix só quando seguro:
   - Backend format: `mvn -f backend/pom.xml spotless:apply`
   - Frontend lint/format: `pnpm --dir frontend exec eslint --fix .` e `pnpm --dir frontend run format`
4. Reexecutar o **mesmo escopo** até passar ou até ficar claro que precisa de decisão do usuário.
5. Não apagar/enfraquecer testes para “passar” coverage ou lint.

### 3. Relatório final (sempre)

```markdown
## Build & Quality — <escopo>

| Check | Resultado |
|---|---|
| … | pass / fail |

**Veredito:** PASS ou FAIL

Se FAIL: causa raiz + arquivos + próximo passo concreto.
Se PASS: mencionar se houve autofix (Spotless/ESLint/Prettier).
```

## Comandos oficiais

### Backend (`backend/`)

Gate completo (testes + Spotless check + PMD + SpotBugs + JaCoCo):

```bash
mvn -f backend/pom.xml clean verify
```

Atalhos úteis:

```bash
mvn -f backend/pom.xml test                 # testes + relatório JaCoCo
mvn -f backend/pom.xml spotless:check       # formatação
mvn -f backend/pom.xml spotless:apply       # corrige formatação
mvn -f backend/pom.xml pmd:check
mvn -f backend/pom.xml spotbugs:check
```

Quality gates embutidos em `verify` (`backend/pom.xml`):

| Ferramenta | Config | Gate |
|---|---|---|
| Spotless | Google Java Format 1.25.2; import order `java\|javax\|jakarta,org,com,br.com.sgd,` | `spotless:check` fail |
| PMD | `backend/config/pmd/ruleset.xml` | `failOnViolation=true` |
| SpotBugs | `backend/config/spotbugs/exclude.xml`, threshold Medium | `failOnError=true` |
| JaCoCo | BUNDLE | linha ≥ 70%, branch ≥ 50% |

Relatório de coverage: `backend/target/site/jacoco/index.html`

Imagem (só escopo `ci`):

```bash
docker build --tag sgd-api:ci backend
```

### Frontend (`frontend/`)

Ordem do CI:

```bash
pnpm --dir frontend install --frozen-lockfile
pnpm --dir frontend run test
pnpm --dir frontend run lint
pnpm --dir frontend run format:check
pnpm --dir frontend run build
```

Scripts:

| Script | O que faz |
|---|---|
| `test` | Vitest |
| `lint` | ESLint (`frontend/eslint.config.js`) |
| `format:check` / `format` | Prettier |
| `build` | `tsc -b && vite build` |

### Infra (só escopo `ci`)

Validar `render.yaml` como no workflow:

```bash
ruby -e "require 'yaml'; spec = YAML.safe_load(File.read('render.yaml'), aliases: true); abort 'missing Render services' unless spec.fetch('services').map { |service| service.fetch('name') }.sort == %w[sgd-api sgd-web]; abort 'missing Render database' unless spec.fetch('databases').map { |database| database.fetch('name') } == %w[sgd-db]"
```

### Hooks locais (contexto; não substituem o gate)

- pre-commit: lint-staged (ESLint/Prettier no frontend; Spotless no Java)
- commit-msg: commitlint conventional (`commitlint.config.cjs`)

## Mapeamento rápido de falhas

| Sintoma | Ação |
|---|---|
| Spotless / formatting | `spotless:apply` ou Prettier `format`, depois re-rodar |
| ESLint import sort / type-imports | `eslint --fix`, revisar manual se restar |
| PMD / SpotBugs | corrigir código; só alterar exclude/ruleset se o usuário pedir |
| JaCoCo abaixo do mínimo | adicionar/ajustar testes; nunca baixar threshold |
| `tsc` no build | corrigir tipos |
| Vitest fail | corrigir código ou teste com comportamento correto |
| Docker build fail | inspecionar `backend/Dockerfile` / contexto |

## Regras

- Espelhar o CI em `.github/workflows/ci.yml` — não pular lint, format, testes ou verify.
- Não sugerir `--no-verify`, desligar plugins ou relaxar JaCoCo/PMD/SpotBugs.
- Em mudanças só de um lado do monorepo, ainda assim preferir o escopo pedido; se o usuário disser “antes do PR”, usar `full` ou `ci`.
- Rodar comandos a partir da raiz do repositório SGD.
- Resposta final concisa: tabela + veredito; detalhes só se houver falha.
