#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../.." && pwd)"
ai_root="${project_root}/services/ai"
venv_bin="${ai_root}/.venv/bin"

usage() {
  echo "Usage: $0 {sync|dev|lint|format|format-check|typecheck|test|eval|check}"
}

require_venv_tool() {
  local tool="$1"

  if [[ ! -x "${venv_bin}/${tool}" ]]; then
    echo "Missing ${venv_bin}/${tool}. Run 'pnpm run ai:sync' first." >&2
    exit 1
  fi
}

run_uv() {
  if command -v uv >/dev/null 2>&1; then
    uv --project "${ai_root}" "$@"
    return
  fi

  if python3 -c 'import uv' >/dev/null 2>&1; then
    python3 -m uv --project "${ai_root}" "$@"
    return
  fi

  echo "uv is required. Install it from https://docs.astral.sh/uv/ and retry." >&2
  exit 1
}

command_name="${1:-}"

case "${command_name}" in
  sync)
    run_uv sync --frozen
    ;;
  dev)
    require_venv_tool uvicorn
    cd "${ai_root}"
    exec "${venv_bin}/uvicorn" docmind_ai.app:create_app --factory --reload \
      --host "${DOCMIND_AI_HOST:-127.0.0.1}" \
      --port "${DOCMIND_AI_PORT:-8090}"
    ;;
  lint)
    require_venv_tool ruff
    "${venv_bin}/ruff" check "${ai_root}/src" "${ai_root}/tests"
    ;;
  format)
    require_venv_tool ruff
    "${venv_bin}/ruff" format "${ai_root}/src" "${ai_root}/tests"
    ;;
  format-check)
    require_venv_tool ruff
    "${venv_bin}/ruff" format --check "${ai_root}/src" "${ai_root}/tests"
    ;;
  typecheck)
    require_venv_tool mypy
    cd "${ai_root}"
    "${venv_bin}/mypy" --config-file pyproject.toml
    ;;
  test)
    require_venv_tool pytest
    cd "${ai_root}"
    "${venv_bin}/pytest" -c pyproject.toml
    ;;
  eval)
    require_venv_tool python
    cd "${ai_root}"
    "${venv_bin}/python" -m docmind_ai.evaluation
    ;;
  check)
    "$0" lint
    "$0" format-check
    "$0" typecheck
    "$0" test
    "$0" eval
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
