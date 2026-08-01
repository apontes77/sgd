#!/usr/bin/env bash
# Quality gate do SGD — espelha .github/workflows/ci.yml
set -euo pipefail

SCOPE="${1:-full}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$ROOT"

RED=$'\033[31m'
GREEN=$'\033[32m'
YELLOW=$'\033[33m'
RESET=$'\033[0m'

pass=0
fail=0
results=()

log() { printf '%s\n' "$*"; }
ok() {
  results+=("PASS|$1")
  pass=$((pass + 1))
  log "${GREEN}PASS${RESET}  $1"
}
ko() {
  results+=("FAIL|$1")
  fail=$((fail + 1))
  log "${RED}FAIL${RESET}  $1"
}

run_check() {
  local name="$1"
  shift
  log ""
  log "${YELLOW}▶${RESET} $name"
  log "  $*"
  if "$@"; then
    ok "$name"
    return 0
  else
    ko "$name"
    return 1
  fi
}

# Não abortar o script inteiro em falha de um check — acumular e falhar no fim.
set +e

run_backend() {
  run_check "backend: mvn clean verify" mvn -f backend/pom.xml clean verify
}

run_frontend() {
  run_check "frontend: pnpm install" pnpm --dir frontend install --frozen-lockfile
  run_check "frontend: test" pnpm --dir frontend run test
  run_check "frontend: lint" pnpm --dir frontend run lint
  run_check "frontend: format:check" pnpm --dir frontend run format:check
  run_check "frontend: build" pnpm --dir frontend run build
}

run_docker() {
  run_check "backend: docker build" docker build --tag sgd-api:ci backend
}

run_infra() {
  run_check "infra: render.yaml" ruby -e "
require 'yaml'
spec = YAML.safe_load(File.read('render.yaml'), aliases: true)
abort 'missing Render services' unless spec.fetch('services').map { |s| s.fetch('name') }.sort == %w[sgd-api sgd-web]
abort 'missing Render database' unless spec.fetch('databases').map { |d| d.fetch('name') } == %w[sgd-db]
"
}

case "$SCOPE" in
  full)
    log "Escopo: full (backend + frontend)"
    run_backend
    run_frontend
    ;;
  backend)
    log "Escopo: backend"
    run_backend
    ;;
  frontend)
    log "Escopo: frontend"
    run_frontend
    ;;
  ci)
    log "Escopo: ci (backend + frontend + docker + render.yaml)"
    run_backend
    run_frontend
    run_docker
    run_infra
    ;;
  *)
    log "Uso: $0 <full|backend|frontend|ci>"
    exit 2
    ;;
esac

set -e

log ""
log "======== Resumo ($SCOPE) ========"
for row in "${results[@]}"; do
  status="${row%%|*}"
  name="${row#*|}"
  if [[ "$status" == "PASS" ]]; then
    log "${GREEN}$status${RESET}  $name"
  else
    log "${RED}$status${RESET}  $name"
  fi
done
log "Total: ${pass} pass, ${fail} fail"

if [[ "$fail" -gt 0 ]]; then
  exit 1
fi
exit 0
