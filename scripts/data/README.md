# Carga de estrutura (masculino / feminino)

Script: `scripts/carga_estrutura.py`  
Compat: `scripts/carga_estrutura_masculina.py` → `--lote masculino`

## Dados

| Lote | Gerentes | Discipulados |
|---|---|---|
| `masculino` | `gerentes_masculino.csv` | `discipulados_masculino.csv` |
| `feminino` | `gerentes_feminino.csv` | `discipulados_feminino.csv` |

## O que carrega

- Gerentes (`GERENTE`, e `DISCIPULADOR` quando também lideram grupo)
- Discipulados do sexo do lote, vinculados às gerências
- **Não** cria co-líderes nem adolescentes
- Em atualização de usuário existente, **preserva** perfis já presentes (ex.: `ADMIN`)

## Pré-voo

```bash
python3 scripts/carga_estrutura.py --lote feminino --validate-only
python3 scripts/carga_estrutura.py --lote masculino --validate-only
```

## Dry-run

```bash
python3 scripts/carga_estrutura.py --lote feminino --dry-run
```

## Homologação / produção

1. Snapshot do Postgres (Dashboard Render → `sgd-db`).
2. Credenciais admin via env ou `.env`:
   - `SGD_ADMIN_EMAIL` / `SGD_ADMIN_PASSWORD`, ou
   - `ADMIN_INITIAL_EMAIL` / `ADMIN_INITIAL_PASSWORD`
3. Dry-run remoto:

```bash
SGD_ADMIN_EMAIL='...' SGD_ADMIN_PASSWORD='...' \
  python3 scripts/carga_estrutura.py \
  --lote feminino \
  --api-url https://<api>/api/v1 \
  --allow-remote \
  --dry-run
```

4. Aplicar com **uma senha temporária igual para todos** (mín. 12 caracteres; não versionar no Git):

```bash
SGD_ADMIN_EMAIL='...' SGD_ADMIN_PASSWORD='...' \
  python3 scripts/carga_estrutura.py \
  --lote feminino \
  --api-url https://<api>/api/v1 \
  --allow-remote \
  --senha-padrao 'SuaFrase12ch' \
  --passwords-out /tmp/sgd-carga-senhas-feminino.csv
```

Sem `--senha-padrao`, o script gera uma senha aleatória por usuário.

5. Avisar os líderes da senha padrão e pedir troca no primeiro acesso.
6. O CSV em `--passwords-out` lista só usuários **novos**. Apague o arquivo depois.
7. Conferir na UI as gerências/discipulados do lote.

Repita com `--lote masculino` se ainda não tiver carregado o lote masculino.

## Idempotência

Reexecutar é seguro: upsert por e-mail (usuário), nome da gerência e `(gerenciaId, nome)` do discipulado.

## Render Blueprint

`render.yaml` do banco de produção: `plan: basic-1gb` e `diskSizeGB: 1`. Após commit, sincronizar o Blueprint no Render.
