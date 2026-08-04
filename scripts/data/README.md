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

4. Aplicar:

```bash
SGD_ADMIN_EMAIL='...' SGD_ADMIN_PASSWORD='...' \
  python3 scripts/carga_estrutura_masculina.py \
  --api-url https://<api>/api/v1 \
  --allow-remote \
  --passwords-out /tmp/sgd-carga-senhas.csv
```

5. Entregar senhas temporárias dos usuários **novos** (arquivo local, permissão `600`). Usuários já existentes não têm senha sobrescrita.
6. Apagar o arquivo de senhas após a entrega.
7. Conferir na UI: 6 gerências, 33 discipulados, papéis acumulados.

## Idempotência

Reexecutar é seguro: upsert por e-mail (usuário), nome da gerência e `(gerenciaId, nome)` do discipulado.

## Render Blueprint

`render.yaml` do banco de produção: `plan: basic-1gb` e `diskSizeGB: 1`. Após commit, sincronizar o Blueprint no Render.
