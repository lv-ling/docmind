#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../.." && pwd)"
ai_root="${project_root}/apps/docmind-document-ai"
venv_bin="${ai_root}/.venv/bin"
venv_python="${venv_bin}/python"

usage() {
  echo "Usage: $0 {sync|dev|lint|format|format-check|typecheck|test|eval|check}"
}

require_venv_python() {
  if [[ ! -x "${venv_python}" ]]; then
    echo "Missing ${venv_python}. Run './scripts/dev/document-ai.sh sync' first." >&2
    exit 1
  fi
}

run_ai_module() {
  require_venv_python
  PYTHONPATH="${ai_root}/src${PYTHONPATH:+:${PYTHONPATH}}" "${venv_python}" -m "$@"
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
    require_venv_python
    cd "${ai_root}"
    exec env PYTHONPATH="${ai_root}/src${PYTHONPATH:+:${PYTHONPATH}}" \
      "${venv_python}" -m uvicorn docmind_ai.app:create_app --factory --reload \
      --host "${DOCMIND_AI_HOST:-127.0.0.1}" \
      --port "${DOCMIND_AI_PORT:-8090}"
    ;;
  lint)
    run_ai_module ruff check "${ai_root}/src" "${ai_root}/tests"
    ;;
  format)
    run_ai_module ruff format "${ai_root}/src" "${ai_root}/tests"
    ;;
  format-check)
    run_ai_module ruff format --check "${ai_root}/src" "${ai_root}/tests"
    ;;
  typecheck)
    cd "${ai_root}"
    run_ai_module mypy --config-file pyproject.toml
    ;;
  test)
    cd "${ai_root}"
    run_ai_module pytest -c pyproject.toml
    ;;
  eval)
    cd "${ai_root}"
    run_ai_module docmind_ai.evaluation
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
