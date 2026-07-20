#!/usr/bin/env python3
"""Cria dados de teste do SGD exclusivamente por meio de uma API local."""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[2]
LOCAL_HOSTS = {"localhost", "127.0.0.1", "::1"}

USERS = (
    {"nome": "Carolina Lacerda", "email": "carol@gmail.com", "senha": "carol@12345678", "perfis": ["GERENTE"]},
    {"nome": "Rebecca Rocha", "email": "rebecca@gmail.com", "senha": "rebecca@123456", "perfis": ["DISCIPULADOR"]},
    {"nome": "Lara Mendonça", "email": "lara@gmail.com", "senha": "lara@1234567", "perfis": ["CO_LIDER"]},
    {"nome": "Lorranny Caetano", "email": "lorranny@gmail.com", "senha": "lorranny@12345", "perfis": ["DISCIPULADOR"]},
    {"nome": "Nathalia Tosta", "email": "nathalia@gmail.com", "senha": "nathalia@123456", "perfis": ["ADMIN"]},
)

GERENCIA_NAME = "Gerência Carolina Lacerda"
DISCIPULADOS = (
    {"nome": "Discipulado Rebecca Rocha", "lider": "rebecca@gmail.com", "co_lideres": ["lara@gmail.com"]},
    {"nome": "Discipulado Lorranny Caetano", "lider": "lorranny@gmail.com", "co_lideres": []},
)


class SeedError(RuntimeError):
    pass


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
            with urlopen(request, timeout=20) as response:
                content = response.read()
                return json.loads(content) if content else None
        except HTTPError as error:
            content = error.read()
            try:
                detail = json.loads(content).get("detail")
            except (json.JSONDecodeError, AttributeError):
                detail = None
            raise SeedError(f"{method} {path} retornou HTTP {error.code}: {detail or error.reason}") from error
        except URLError as error:
            raise SeedError(f"Não foi possível acessar {self.base_url}: {error.reason}") from error

    def login(self, email: str, password: str) -> dict[str, Any]:
        session = self.request("POST", "/autenticacao/login", {"email": email, "senha": password})
        self.access_token = session["accessToken"]
        return session["usuario"]


def local_api_url(value: str) -> str:
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or parsed.hostname not in LOCAL_HOSTS:
        raise argparse.ArgumentTypeError("o seed aceita somente URLs locais (localhost, 127.0.0.1 ou ::1)")
    if parsed.username or parsed.password:
        raise argparse.ArgumentTypeError("não informe credenciais na URL")
    return value.rstrip("/")


def page(client: ApiClient, path: str) -> list[dict[str, Any]]:
    separator = "&" if "?" in path else "?"
    response = client.request("GET", f"{path}{separator}{urlencode({'page': 0, 'size': 100})}")
    return response["content"]


def upsert_users(client: ApiClient) -> dict[str, dict[str, Any]]:
    existing = {item["email"].lower(): item for item in page(client, "/usuarios")}
    result: dict[str, dict[str, Any]] = {}
    for expected in USERS:
        email = expected["email"].lower()
        current = existing.get(email)
        if current is None:
            current = client.request("POST", "/usuarios", expected)
            print(f"[criado] usuário {expected['nome']}")
        else:
            current = client.request("PATCH", f"/usuarios/{current['id']}", {
                "nome": expected["nome"], "perfis": expected["perfis"], "ativo": True,
            })
            print(f"[atualizado] usuário {expected['nome']}")
        result[email] = current
    return result


def upsert_gerencia(client: ApiClient, users: dict[str, dict[str, Any]]) -> dict[str, Any]:
    manager_id = users["carol@gmail.com"]["id"]
    gerencias = page(client, "/gerencias")
    current = next((item for item in gerencias if item["nome"].casefold() == GERENCIA_NAME.casefold()), None)
    if current is None:
        current = client.request("POST", "/gerencias", {"nome": GERENCIA_NAME, "gerenteId": manager_id})
        print(f"[criada] {GERENCIA_NAME}")
    else:
        current = client.request("PATCH", f"/gerencias/{current['id']}", {
            "nome": GERENCIA_NAME, "gerenteId": manager_id, "ativo": True,
        })
        print(f"[atualizada] {GERENCIA_NAME}")
    return current


def upsert_discipulados(
    client: ApiClient, users: dict[str, dict[str, Any]], gerencia: dict[str, Any]
) -> None:
    existing = {item["nome"].casefold(): item for item in page(client, "/discipulados")}
    for expected in DISCIPULADOS:
        leader_id = users[expected["lider"]]["id"]
        body = {
            "nome": expected["nome"],
            "sexo": "FEMININO",
            "gerenciaId": gerencia["id"],
            "discipuladorId": leader_id,
        }
        current = existing.get(expected["nome"].casefold())
        if current is None:
            current = client.request("POST", "/discipulados", body)
            print(f"[criado] {expected['nome']}")
        else:
            current = client.request("PATCH", f"/discipulados/{current['id']}", {**body, "ativo": True})
            print(f"[atualizado] {expected['nome']}")
        co_leader_ids = [users[email]["id"] for email in expected["co_lideres"]]
        client.request("PUT", f"/discipulados/{current['id']}/co-lideres", {"usuarioIds": co_leader_ids})


def verify_logins(base_url: str) -> None:
    for expected in USERS:
        user = ApiClient(base_url).login(expected["email"], expected["senha"])
        if not set(expected["perfis"]).issubset(user.get("perfis", [])):
            raise SeedError(f"o usuário {expected['nome']} autenticou sem o perfil esperado")
        print(f"[login OK] {expected['nome']}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Cria usuários e estrutura de teste em uma instância local do SGD.")
    parser.add_argument("--api-url", type=local_api_url, default="http://localhost:5173/api/v1")
    parser.add_argument("--env-file", type=Path, default=ROOT / ".env")
    args = parser.parse_args()

    dotenv = read_dotenv(args.env_file)
    admin_email = os.getenv("SGD_ADMIN_EMAIL") or dotenv.get("ADMIN_INITIAL_EMAIL")
    admin_password = os.getenv("SGD_ADMIN_PASSWORD") or dotenv.get("ADMIN_INITIAL_PASSWORD")
    if not admin_email or not admin_password:
        raise SeedError(
            "informe SGD_ADMIN_EMAIL e SGD_ADMIN_PASSWORD ou configure "
            "ADMIN_INITIAL_EMAIL e ADMIN_INITIAL_PASSWORD no .env"
        )

    client = ApiClient(args.api_url)
    admin = client.login(admin_email, admin_password)
    if "ADMIN" not in admin.get("perfis", []):
        raise SeedError("as credenciais informadas não pertencem a um ADMIN")

    users = upsert_users(client)
    gerencia = upsert_gerencia(client, users)
    upsert_discipulados(client, users, gerencia)
    verify_logins(args.api_url)
    print("Seed local concluído com sucesso.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except SeedError as error:
        print(f"Erro: {error}", file=sys.stderr)
        raise SystemExit(1)
