#!/usr/bin/env python3
"""Carga idempotente da estrutura masculina (gerentes + discipuladores) via API admin.

Lê os CSVs em scripts/data/, valida, faz dry-run ou aplica em ambiente autorizado.
Não usa Flyway: respeita regras de domínio da API.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import secrets
import string
import sys
import unicodedata
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = Path(__file__).resolve().parent / "data"
LOCAL_HOSTS = {"localhost", "127.0.0.1", "::1"}

FAIXA_MAP = {
    "9-11": "DE_09_A_11",
    "09-11": "DE_09_A_11",
    "11-13": "DE_11_A_13",
    "13-15": "DE_13_A_15",
    "15+": "DE_15_MAIS",
    "15mais": "DE_15_MAIS",
}

# Chave da coluna GERÊNCIA no CSV de discipulados → e-mail do gerente
GERENCIA_ALIASES: dict[str, str] = {
    "joao paulo": "jpos.joaopaulo@gmail.com",
    "william": "willianalves306@gmail.com",
    "matheus maia": "matheusbatera09@gmail.com",
    "asaf": "asafalmeida2013@gmail.com",
    "nicolas": "nicolasrichard62@gmail.com",
    "pastor leo": "leodokmos@gmail.com",
    "leonardo": "leodokmos@gmail.com",
}

EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


class CargaError(RuntimeError):
    pass


def fold(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value.strip())
    without_accents = "".join(ch for ch in normalized if not unicodedata.combining(ch))
    return re.sub(r"\s+", " ", without_accents).casefold()


def clean(value: str | None) -> str:
    return (value or "").strip()


def normalize_email(value: str) -> str:
    return clean(value).lower()


def map_faixa(raw: str) -> str:
    key = re.sub(r"\s+", "", clean(raw).casefold()).replace("–", "-").replace("—", "-")
    if key not in FAIXA_MAP:
        raise CargaError(f"faixa etária inválida: {raw!r}")
    return FAIXA_MAP[key]


def map_sexo(raw: str) -> str:
    key = fold(raw)
    if key in {"masculino", "m"}:
        return "MASCULINO"
    if key in {"feminino", "f"}:
        return "FEMININO"
    raise CargaError(f"sexo inválido: {raw!r}")


def generate_password(length: int = 16) -> str:
    alphabet = string.ascii_letters + string.digits + "!@#$%&*"
    while True:
        password = "".join(secrets.choice(alphabet) for _ in range(length))
        if (
            any(c.islower() for c in password)
            and any(c.isupper() for c in password)
            and any(c.isdigit() for c in password)
            and any(c in "!@#$%&*" for c in password)
        ):
            return password


def read_dotenv(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        if line.startswith("export "):
            line = line[7:].lstrip()
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
            value = value[1:-1]
        values[key.strip()] = value
    return values


@dataclass
class GerenteRow:
    nome: str
    email: str
    sexo: str
    faixa_csv: str


@dataclass
class DiscipuladoRow:
    gerencia_key: str
    discipulador_nome: str
    email: str
    nome: str
    faixa: str
    observacao: str
    gerente_email: str = ""


@dataclass
class CargaPlan:
    gerentes: list[GerenteRow]
    discipulados: list[DiscipuladoRow]
    users: dict[str, dict[str, Any]] = field(default_factory=dict)
    gerencias: dict[str, dict[str, Any]] = field(default_factory=dict)
    warnings: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)


class ApiClient:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.access_token: str | None = None

    def request(self, method: str, path: str, body: Any | None = None) -> Any:
        headers = {"Accept": "application/json"}
        data = None
        if body is not None:
            headers["Content-Type"] = "application/json"
            data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        if self.access_token:
            headers["Authorization"] = f"Bearer {self.access_token}"
        request = Request(f"{self.base_url}{path}", data=data, headers=headers, method=method)
        try:
            with urlopen(request, timeout=30) as response:
                content = response.read()
                return json.loads(content) if content else None
        except HTTPError as error:
            content = error.read()
            try:
                payload = json.loads(content)
                detail = payload.get("detail") or payload.get("title") or payload
            except (json.JSONDecodeError, AttributeError):
                detail = None
            raise CargaError(f"{method} {path} retornou HTTP {error.code}: {detail or error.reason}") from error
        except URLError as error:
            raise CargaError(f"Não foi possível acessar {self.base_url}: {error.reason}") from error

    def login(self, email: str, password: str) -> dict[str, Any]:
        session = self.request("POST", "/autenticacao/login", {"email": email, "senha": password})
        self.access_token = session["accessToken"]
        return session["usuario"]


def api_url(value: str) -> str:
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname:
        raise argparse.ArgumentTypeError("informe uma URL HTTP(S) válida")
    if parsed.username or parsed.password:
        raise argparse.ArgumentTypeError("não informe credenciais na URL")
    return value.rstrip("/")


def validate_environment(base_url: str, allow_remote: bool) -> None:
    parsed = urlparse(base_url)
    is_local = parsed.hostname in LOCAL_HOSTS
    if not is_local and not allow_remote:
        raise CargaError("URLs remotas exigem a opção --allow-remote")
    if not is_local and parsed.scheme != "https":
        raise CargaError("ambientes remotos exigem HTTPS para proteger credenciais e tokens")


def page_all(client: ApiClient, path: str) -> list[dict[str, Any]]:
    items: list[dict[str, Any]] = []
    page_idx = 0
    while True:
        separator = "&" if "?" in path else "?"
        response = client.request(
            "GET", f"{path}{separator}{urlencode({'page': page_idx, 'size': 100})}"
        )
        content = response["content"]
        items.extend(content)
        if page_idx >= response.get("totalPages", 1) - 1 or not content:
            break
        page_idx += 1
    return items


def load_gerentes(path: Path) -> list[GerenteRow]:
    rows: list[GerenteRow] = []
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        for raw in reader:
            normalized = {fold(k): clean(v) for k, v in raw.items() if k is not None}
            nome = normalized.get("nome") or ""
            email = normalize_email(normalized.get("email") or "")
            sexo = normalized.get("sexo") or ""
            faixa = normalized.get("idade da gerencia") or ""
            if not any([nome, email, sexo, faixa]):
                continue
            rows.append(GerenteRow(nome=nome, email=email, sexo=sexo, faixa_csv=faixa))
    return rows


def load_discipulados(path: Path) -> list[DiscipuladoRow]:
    rows: list[DiscipuladoRow] = []
    with path.open(encoding="utf-8-sig", newline="") as handle:
        lines = handle.readlines()
    # Pula título e linha vazia; header na 3ª linha (índice 2)
    header_idx = next(
        (i for i, line in enumerate(lines) if fold(line.split(",")[0]).startswith("gerencia")),
        None,
    )
    if header_idx is None:
        raise CargaError(f"cabeçalho GERÊNCIA não encontrado em {path}")
    reader = csv.DictReader(lines[header_idx:])
    for raw in reader:
        normalized = {fold(k): clean(v) for k, v in raw.items() if k is not None}
        gerencia_key = normalized.get("gerencia") or ""
        discipulador = normalized.get("discipulador") or ""
        email = normalize_email(normalized.get("email") or "")
        nome = normalized.get("nome do discipulado") or ""
        faixa = normalized.get("faixa etaria do discipulado") or ""
        obs = normalized.get("observacoes") or ""
        if not any([gerencia_key, discipulador, email, nome, faixa]):
            continue
        if not nome or fold(nome) in {"sem nome", "sem-nome"}:
            nome = f"Discipulado {discipulador}"
        rows.append(
            DiscipuladoRow(
                gerencia_key=gerencia_key,
                discipulador_nome=discipulador,
                email=email,
                nome=nome,
                faixa=faixa,
                observacao=obs,
            )
        )
    return rows


def build_plan(gerentes_path: Path, discipulados_path: Path) -> CargaPlan:
    gerentes = load_gerentes(gerentes_path)
    discipulados = load_discipulados(discipulados_path)
    plan = CargaPlan(gerentes=gerentes, discipulados=discipulados)

    gerente_by_email = {g.email: g for g in gerentes}
    for email, gerente in list(gerente_by_email.items()):
        if not email or not EMAIL_RE.match(email):
            plan.errors.append(f"gerente com e-mail inválido: {gerente.nome!r} / {email!r}")
        try:
            map_sexo(gerente.sexo)
        except CargaError as error:
            plan.errors.append(str(error))

    if len(gerente_by_email) != len(gerentes):
        plan.errors.append("há e-mails duplicados no CSV de gerentes")

    for row in discipulados:
        key = fold(row.gerencia_key)
        gerente_email = GERENCIA_ALIASES.get(key)
        if not gerente_email:
            plan.errors.append(f"gerência sem alias: {row.gerencia_key!r} (discipulado {row.nome})")
            continue
        if gerente_email not in gerente_by_email:
            plan.errors.append(
                f"gerência {row.gerencia_key!r} aponta para {gerente_email}, ausente no CSV de gerentes"
            )
            continue
        row.gerente_email = gerente_email
        if not row.email or not EMAIL_RE.match(row.email):
            plan.errors.append(f"discipulador com e-mail inválido: {row.discipulador_nome!r} / {row.email!r}")
        try:
            map_faixa(row.faixa)
        except CargaError as error:
            plan.errors.append(f"{row.nome}: {error}")
        if row.observacao:
            plan.warnings.append(f"{row.nome} ({row.email}): {row.observacao}")

    emails = [d.email for d in discipulados if d.email]
    if len(emails) != len(set(emails)):
        seen: set[str] = set()
        for email in emails:
            if email in seen:
                plan.errors.append(f"e-mail de discipulador duplicado: {email}")
            seen.add(email)

    # Usuários: união de gerentes + discipuladores, com perfis acumulados
    users: dict[str, dict[str, Any]] = {}
    for gerente in gerentes:
        users[gerente.email] = {
            "nome": gerente.nome,
            "email": gerente.email,
            "perfis": {"GERENTE"},
            "sexo_ref": map_sexo(gerente.sexo) if gerente.sexo else "MASCULINO",
        }
    for row in discipulados:
        if not row.email:
            continue
        current = users.get(row.email)
        if current is None:
            users[row.email] = {
                "nome": row.discipulador_nome,
                "email": row.email,
                "perfis": {"DISCIPULADOR"},
            }
        else:
            current["perfis"].add("DISCIPULADOR")
            # Preferir nome do CSV de gerentes se já existir; senão usar discipulador
            if "GERENTE" not in current["perfis"]:
                current["nome"] = row.discipulador_nome

    for user in users.values():
        user["perfis"] = sorted(user["perfis"])

    # Gerências: nome + faixas = união das faixas dos discipulados
    gerencias: dict[str, dict[str, Any]] = {}
    for gerente in gerentes:
        gerencias[gerente.email] = {
            "nome": f"Gerência {gerente.nome}",
            "gerente_email": gerente.email,
            "sexo": map_sexo(gerente.sexo) if gerente.sexo else "MASCULINO",
            "faixas": set(),
        }
    for row in discipulados:
        if not row.gerente_email or row.gerente_email not in gerencias:
            continue
        try:
            gerencias[row.gerente_email]["faixas"].add(map_faixa(row.faixa))
        except CargaError:
            pass

    for g in gerencias.values():
        if not g["faixas"]:
            plan.errors.append(f"gerência sem discipulados/faixas: {g['nome']}")
        g["faixas"] = sorted(g["faixas"])

    # Conflito de nome de discipulado na mesma gerência
    seen_names: set[tuple[str, str]] = set()
    for row in discipulados:
        key = (row.gerente_email, fold(row.nome))
        if key in seen_names:
            plan.errors.append(
                f"nome de discipulado duplicado na gerência {row.gerencia_key}: {row.nome}"
            )
        seen_names.add(key)

    plan.users = users
    plan.gerencias = gerencias
    return plan


def print_preflight(plan: CargaPlan) -> None:
    print("=== Pré-voo ===")
    print(f"Gerentes: {len(plan.gerentes)}")
    print(f"Discipulados: {len(plan.discipulados)}")
    print(f"Usuários únicos: {len(plan.users)}")
    print(f"Gerências: {len(plan.gerencias)}")
    dual = [u for u in plan.users.values() if set(u["perfis"]) >= {"GERENTE", "DISCIPULADOR"}]
    print(f"Usuários GERENTE+DISCIPULADOR: {len(dual)}")
    for g in plan.gerencias.values():
        print(f"  - {g['nome']}: faixas={g['faixas']} gerente={g['gerente_email']}")
    if plan.warnings:
        print(f"\nObservações ({len(plan.warnings)}):")
        for warning in plan.warnings:
            print(f"  ! {warning}")
    if plan.errors:
        print(f"\nErros bloqueantes ({len(plan.errors)}):")
        for error in plan.errors:
            print(f"  x {error}")


def upsert_users(
    client: ApiClient | None,
    plan: CargaPlan,
    *,
    dry_run: bool,
    passwords: dict[str, str],
) -> dict[str, dict[str, Any]]:
    existing: dict[str, dict[str, Any]] = {}
    if client is not None and not dry_run:
        existing = {item["email"].lower(): item for item in page_all(client, "/usuarios")}

    result: dict[str, dict[str, Any]] = {}
    for email, expected in sorted(plan.users.items()):
        body_perfis = expected["perfis"]
        current = existing.get(email)
        if current is None:
            if dry_run or client is None:
                print(f"[dry-run] criar usuário {expected['nome']} <{email}> perfis={body_perfis}")
                result[email] = {"id": None, **expected}
                continue
            password = passwords[email]
            current = client.request(
                "POST",
                "/usuarios",
                {
                    "nome": expected["nome"],
                    "email": email,
                    "senha": password,
                    "perfis": body_perfis,
                },
            )
            print(f"[criado] usuário {expected['nome']} <{email}>")
        else:
            if dry_run or client is None:
                print(f"[dry-run] atualizar usuário {expected['nome']} <{email}> perfis={body_perfis}")
                result[email] = current
                continue
            current = client.request(
                "PATCH",
                f"/usuarios/{current['id']}",
                {"nome": expected["nome"], "perfis": body_perfis, "ativo": True},
            )
            print(f"[atualizado] usuário {expected['nome']} <{email}>")
        result[email] = current
    return result


def upsert_gerencias(
    client: ApiClient | None,
    plan: CargaPlan,
    users: dict[str, dict[str, Any]],
    *,
    dry_run: bool,
) -> dict[str, dict[str, Any]]:
    existing_list: list[dict[str, Any]] = []
    if client is not None and not dry_run:
        existing_list = page_all(client, "/gerencias")
    existing = {item["nome"].casefold(): item for item in existing_list}
    result: dict[str, dict[str, Any]] = {}

    for email, expected in plan.gerencias.items():
        nome = expected["nome"]
        gerente = users[email]
        body = {
            "nome": nome,
            "sexo": expected["sexo"],
            "faixasEtarias": expected["faixas"],
            "gerenteId": gerente.get("id"),
        }
        current = existing.get(nome.casefold())
        if current is None:
            if dry_run or client is None:
                print(f"[dry-run] criar gerência {nome} faixas={expected['faixas']}")
                result[email] = {"id": None, **expected}
                continue
            current = client.request("POST", "/gerencias", body)
            print(f"[criada] {nome}")
        else:
            if dry_run or client is None:
                print(f"[dry-run] atualizar gerência {nome} faixas={expected['faixas']}")
                result[email] = current
                continue
            current = client.request(
                "PATCH",
                f"/gerencias/{current['id']}",
                {**body, "ativo": True},
            )
            print(f"[atualizada] {nome}")
        result[email] = current
    return result


def upsert_discipulados(
    client: ApiClient | None,
    plan: CargaPlan,
    users: dict[str, dict[str, Any]],
    gerencias: dict[str, dict[str, Any]],
    *,
    dry_run: bool,
) -> None:
    existing_list: list[dict[str, Any]] = []
    if client is not None and not dry_run:
        existing_list = page_all(client, "/discipulados")
    # Chave: (gerenciaId, nome.casefold()) quando ids existem; fallback por nome global no dry-run
    existing_by_key: dict[tuple[Any, str], dict[str, Any]] = {}
    for item in existing_list:
        existing_by_key[(item.get("gerenciaId"), item["nome"].casefold())] = item

    for row in plan.discipulados:
        gerencia = gerencias[row.gerente_email]
        lider = users[row.email]
        faixa = map_faixa(row.faixa)
        body = {
            "nome": row.nome,
            "sexo": "MASCULINO",
            "faixaEtaria": faixa,
            "gerenciaId": gerencia.get("id"),
            "discipuladorId": lider.get("id"),
        }
        key = (gerencia.get("id"), row.nome.casefold())
        current = existing_by_key.get(key)
        if current is None and gerencia.get("id") is None:
            # dry-run sem ids: só reporta criação
            current = None
        if current is None:
            if dry_run or client is None:
                print(
                    f"[dry-run] criar discipulado {row.nome!r} "
                    f"em {gerencia['nome']} faixa={faixa} lider={row.email}"
                )
                continue
            current = client.request("POST", "/discipulados", body)
            print(f"[criado] discipulado {row.nome}")
        else:
            if dry_run or client is None:
                print(
                    f"[dry-run] atualizar discipulado {row.nome!r} "
                    f"em {gerencia['nome']} faixa={faixa} lider={row.email}"
                )
                continue
            current = client.request(
                "PATCH",
                f"/discipulados/{current['id']}",
                {**body, "ativo": True},
            )
            print(f"[atualizado] discipulado {row.nome}")


def write_passwords(path: Path, passwords: dict[str, str], plan: CargaPlan) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["nome", "email", "perfis", "senha_temporaria"])
        for email, password in sorted(passwords.items()):
            user = plan.users[email]
            writer.writerow([user["nome"], email, "|".join(user["perfis"]), password])
    try:
        os.chmod(path, 0o600)
    except OSError:
        pass


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Carga da estrutura masculina (gerentes + discipuladores) via API."
    )
    parser.add_argument("--api-url", type=api_url, default="http://localhost:5173/api/v1")
    parser.add_argument(
        "--allow-remote",
        action="store_true",
        help="confirma que a API remota é um ambiente autorizado",
    )
    parser.add_argument("--env-file", type=Path, default=ROOT / ".env")
    parser.add_argument(
        "--gerentes-csv",
        type=Path,
        default=DATA_DIR / "gerentes_masculino.csv",
    )
    parser.add_argument(
        "--discipulados-csv",
        type=Path,
        default=DATA_DIR / "discipulados_masculino.csv",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="valida e mostra o plano sem mutar o banco",
    )
    parser.add_argument(
        "--validate-only",
        action="store_true",
        help="somente pré-voo local (sem login na API)",
    )
    parser.add_argument(
        "--passwords-out",
        type=Path,
        default=None,
        help="arquivo CSV local com senhas temporárias (fora do repo)",
    )
    parser.add_argument(
        "--senha-padrao",
        default=None,
        help=(
            "senha temporária única para todos os usuários novos "
            "(mín. 12 caracteres; recomendado para carga em massa)"
        ),
    )
    args = parser.parse_args()

    if not args.gerentes_csv.exists() or not args.discipulados_csv.exists():
        raise CargaError("CSVs de entrada não encontrados em scripts/data/")

    plan = build_plan(args.gerentes_csv, args.discipulados_csv)
    print_preflight(plan)
    if plan.errors:
        raise CargaError(f"pré-voo falhou com {len(plan.errors)} erro(s)")

    if args.validate_only:
        print("\nValidação local OK.")
        return 0

    validate_environment(args.api_url, args.allow_remote)

    if args.senha_padrao is not None:
        if len(args.senha_padrao) < 12:
            raise CargaError("--senha-padrao deve ter no mínimo 12 caracteres (regra da API)")
        shared = args.senha_padrao
        passwords = {email: shared for email in plan.users}
        print(f"Senha temporária: padrão compartilhada ({len(shared)} caracteres).")
    else:
        passwords = {email: generate_password() for email in plan.users}
        print("Senha temporária: única gerada por usuário (use --senha-padrao para uma só).")

    if args.dry_run:
        print("\n=== Dry-run (sem mutações) ===")
        upsert_users(None, plan, dry_run=True, passwords=passwords)
        upsert_gerencias(None, plan, plan.users, dry_run=True)
        upsert_discipulados(None, plan, plan.users, plan.gerencias, dry_run=True)
        print("\nDry-run concluído.")
        return 0

    dotenv = read_dotenv(args.env_file)
    admin_email = os.getenv("SGD_ADMIN_EMAIL") or dotenv.get("ADMIN_INITIAL_EMAIL")
    admin_password = os.getenv("SGD_ADMIN_PASSWORD") or dotenv.get("ADMIN_INITIAL_PASSWORD")
    if not admin_email or not admin_password:
        raise CargaError(
            "informe SGD_ADMIN_EMAIL e SGD_ADMIN_PASSWORD ou configure "
            "ADMIN_INITIAL_EMAIL e ADMIN_INITIAL_PASSWORD no .env"
        )

    client = ApiClient(args.api_url)
    admin = client.login(admin_email, admin_password)
    if "ADMIN" not in admin.get("perfis", []):
        raise CargaError("as credenciais informadas não pertencem a um ADMIN")

    # Senhas só para usuários novos; descobrir existentes antes
    existing_users = {item["email"].lower() for item in page_all(client, "/usuarios")}
    new_passwords = {email: pwd for email, pwd in passwords.items() if email not in existing_users}

    users = upsert_users(client, plan, dry_run=False, passwords=passwords)
    gerencias = upsert_gerencias(client, plan, users, dry_run=False)
    upsert_discipulados(client, plan, users, gerencias, dry_run=False)

    if new_passwords:
        if args.senha_padrao:
            print(
                f"\n{len(new_passwords)} usuário(s) novo(s) com a mesma senha temporária "
                "(avise para alterarem no primeiro acesso)."
            )
        out = args.passwords_out
        if out is None:
            stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
            out = Path(f"/tmp/sgd-carga-senhas-{stamp}.csv")
        subset = CargaPlan(
            gerentes=plan.gerentes,
            discipulados=plan.discipulados,
            users={e: plan.users[e] for e in new_passwords},
        )
        write_passwords(out, new_passwords, subset)
        print(f"Lista dos usuários novos: {out}")
        print("Entregue fora de banda e apague o arquivo depois.")
    else:
        print("\nNenhum usuário novo: senhas existentes preservadas.")

    print("Carga da estrutura masculina concluída.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except CargaError as error:
        print(f"Erro: {error}", file=sys.stderr)
        raise SystemExit(1)
