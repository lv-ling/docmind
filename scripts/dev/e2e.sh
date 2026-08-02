#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../.." && pwd)"

usage() {
  echo "Usage: $0 {mvp|source-formats} [arguments...]"
}

command_name="${1:-}"
if [[ -n "${command_name}" ]]; then
  shift
fi

case "${command_name}" in
  mvp)
    exec python3 "${project_root}/tests/e2e/template_flow.py" "$@"
    ;;
  source-formats)
    exec python3 "${project_root}/tests/e2e/source_formats.py" "$@"
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
