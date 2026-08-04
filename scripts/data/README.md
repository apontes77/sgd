# Carga da estrutura masculina (produção / homolog)

Script: `scripts/carga_estrutura_masculina.py`  
Dados: `scripts/data/gerentes_masculino.csv` e `scripts/data/discipulados_masculino.csv`

## O que carrega

- 6 gerentes (`GERENTE`, e `DISCIPULADOR` quando também lideram grupo)
- 33 discipulados masculinos vinculados às gerências
- **Não** cria co-líderes nem adolescentes

## Pré-voo (sem API)

```bash
python3 scripts/carga_estrutura_masculina.py --validate-only
```

## Dry-run (plano sem mutar)

```bash
python3 scripts/carga_estrutura_masculina.py --dry-run
```

## Homologação / produção

1. Snapshot do Postgres (Dashboard Render → `sgd-db`).
2. Credenciais admin via env ou `.env`:
   - `SGD_ADMIN_EMAIL` / `SGD_ADMIN_PASSWORD`, ou
   - `ADMIN_INITIAL_EMAIL` / `ADMIN_INITIAL_PASSWORD`
3. Dry-run remoto:

```bash
SGD_ADMIN_EMAIL='...' SGD_ADMIN_PASSWORD='...' \
  python3 scripts/carga_estrutura_masculina.py \
  --api-url https://<api>/api/v1 \
  --allow-remote \
  --dry-run
```

4. Aplicar com **uma senha temporária igual para todos** (mín. 12 caracteres; não versionar no Git):

```bash
SGD_ADMIN_EMAIL='...' SGD_ADMIN_PASSWORD='...' \
  python3 scripts/carga_estrutura_masculina.py \
  --api-url https://<api>/api/v1 \
  --allow-remote \
  --senha-padrao 'SuaFrase12ch' \
  --passwords-out /tmp/sgd-carga-senhas.csv
```

Sem `--senha-padrao`, o script gera uma senha aleatória por usuário.

5. Avisar os líderes da senha padrão e pedir troca no primeiro acesso (`esqueci a senha` ou redefinição).
6. O CSV em `--passwords-out` lista só usuários **novos** (existentes não têm senha alterada). Apague o arquivo depois.
7. Conferir na UI: 6 gerências, 33 discipulados, papéis acumulados.

## Idempotência

Reexecutar é seguro: upsert por e-mail (usuário), nome da gerência e `(gerenciaId, nome)` do discipulado.

## Render Blueprint

`render.yaml` do banco de produção: `plan: basic-1gb` e `diskSizeGB: 1`. Após commit, sincronizar o Blueprint no Render.
