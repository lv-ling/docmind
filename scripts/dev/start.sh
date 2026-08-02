#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../.." && pwd)"
web_root="${project_root}/apps/docmind-web"
ai_root="${project_root}/apps/docmind-document-ai"
server_root="${project_root}/apps/docmind-server"

child_names=()
child_pids=()

usage() {
  echo "Usage: $0"
  echo "Start infrastructure and the Web, Server, and Document AI development processes."
}

require_command() {
  local command_name="$1"
  local install_hint="$2"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Missing ${command_name}. ${install_hint}" >&2
    exit 1
  fi
}

preflight() {
  require_command docker "Install and start Docker Desktop."
  require_command pnpm "Install pnpm 10.13.1."
  require_command java "Install Java 17."

  if [[ ! -d "${web_root}/node_modules" ]]; then
    echo "Web dependencies are missing. Run './scripts/dev/web.sh install' first." >&2
    exit 1
  fi
  if [[ ! -x "${ai_root}/.venv/bin/python" ]]; then
    echo "Document AI environment is missing. Run './scripts/dev/document-ai.sh sync' first." >&2
    exit 1
  fi
  if [[ ! -x "${server_root}/mvnw" ]]; then
    echo "Server Maven Wrapper is missing or not executable: ${server_root}/mvnw" >&2
    exit 1
  fi
  if ! docker info >/dev/null 2>&1; then
    echo "Docker is installed but the daemon is unavailable. Start Docker Desktop and retry." >&2
    exit 1
  fi
}

start_child() {
  local name="$1"
  shift
  "$@" &
  child_names+=("${name}")
  child_pids+=("$!")
}

cleanup() {
  local exit_status="$?"
  trap - EXIT INT TERM
  if (( ${#child_pids[@]} > 0 )); then
    echo
    echo "Stopping DocMind application processes..."
    kill "${child_pids[@]}" 2>/dev/null || true
    for pid in "${child_pids[@]}"; do
      wait "${pid}" 2>/dev/null || true
    done
  fi
  echo "Infrastructure containers remain running. Stop them with './scripts/dev/infra.sh stop'."
  exit "${exit_status}"
}

handle_interrupt() {
  exit 130
}

handle_termination() {
  exit 143
}

case "${1:-}" in
  "") ;;
  -h|--help)
    usage
    exit 0
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac

preflight
"${script_dir}/infra.sh" start

trap cleanup EXIT
trap handle_interrupt INT
trap handle_termination TERM

start_child "Document AI" "${script_dir}/document-ai.sh" dev
start_child "Server" "${script_dir}/server.sh" dev
start_child "Web" "${script_dir}/web.sh" dev

echo
echo "DocMind development stack is starting:"
echo "  Web:         http://127.0.0.1:5173"
echo "  Server:      http://127.0.0.1:8080"
echo "  Document AI: http://127.0.0.1:8090"
echo "Press Ctrl+C to stop the three applications. Infrastructure will be kept running."

while true; do
  for index in "${!child_pids[@]}"; do
    pid="${child_pids[${index}]}"
    if ! kill -0 "${pid}" 2>/dev/null; then
      set +e
      wait "${pid}"
      child_status="$?"
      set -e
      echo "${child_names[${index}]} exited with status ${child_status}." >&2
      exit "${child_status}"
    fi
  done
  sleep 1
done
