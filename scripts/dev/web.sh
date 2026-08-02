#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../.." && pwd)"
web_root="${project_root}/apps/docmind-web"

usage() {
  echo "Usage: $0 {install|dev|build|lint|format|format-check|typecheck|test|test-watch|test-coverage|check}"
}

if ! command -v pnpm >/dev/null 2>&1; then
  echo "pnpm is required. Install pnpm 10.13.1 and retry." >&2
  exit 1
fi

case "${1:-}" in
  install)
    pnpm --dir "${web_root}" install --frozen-lockfile
    ;;
  dev)
    exec pnpm --dir "${web_root}" run dev
    ;;
  build|lint|format|typecheck|test|check)
    pnpm --dir "${web_root}" run "$1"
    ;;
  format-check)
    pnpm --dir "${web_root}" run format:check
    ;;
  test-watch)
    pnpm --dir "${web_root}" run test:watch
    ;;
  test-coverage)
    pnpm --dir "${web_root}" run test:coverage
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
