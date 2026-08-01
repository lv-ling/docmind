#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose_file="$project_root/deploy/compose/docker-compose.yml"
compose_dir="$(dirname "$compose_file")"
compose=(docker compose)
if [[ -f "$compose_dir/.env" ]]; then
  compose+=(--env-file "$compose_dir/.env")
fi
compose+=(-f "$compose_file")

wait_for_health() {
  local deadline=$((SECONDS + 60))
  local container status
  while (( SECONDS < deadline )); do
    local ready=1
    for container in docmind-postgres docmind-redis docmind-rabbitmq docmind-minio; do
      status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"
      if [[ "$status" != "healthy" ]]; then
        ready=0
      fi
    done
    if (( ready == 1 )); then
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for infrastructure health checks." >&2
  "${compose[@]}" ps
  return 1
}

wait_for_editor_health() {
  local deadline=$((SECONDS + 240))
  local status
  while (( SECONDS < deadline )); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' docmind-onlyoffice 2>/dev/null || true)"
    if [[ "$status" == "healthy" ]]; then
      return 0
    fi
    sleep 3
  done
  echo "Timed out waiting for ONLYOFFICE health check." >&2
  "${compose[@]}" --profile editor ps onlyoffice
  return 1
}

run_minio_init() {
  "${compose[@]}" --profile init run --rm --no-deps minio-init
}

case "${1:-status}" in
  start)
    "${compose[@]}" up -d
    wait_for_health
    run_minio_init
    "${compose[@]}" ps
    ;;
  stop)
    "${compose[@]}" stop
    ;;
  restart)
    "${compose[@]}" restart
    wait_for_health
    "${compose[@]}" ps
    ;;
  recreate)
    "${compose[@]}" up -d --force-recreate
    wait_for_health
    run_minio_init
    "${compose[@]}" ps
    ;;
  status)
    "${compose[@]}" ps
    ;;
  logs)
    if [[ $# -ge 2 ]]; then
      "${compose[@]}" logs -f "$2"
    else
      "${compose[@]}" logs -f
    fi
    ;;
  editor-start)
    "${compose[@]}" --profile editor up -d onlyoffice
    wait_for_editor_health
    "${compose[@]}" --profile editor ps onlyoffice
    ;;
  editor-stop)
    "${compose[@]}" --profile editor stop onlyoffice
    ;;
  editor-status)
    "${compose[@]}" --profile editor ps onlyoffice
    ;;
  editor-fonts)
    "${compose[@]}" --profile editor exec -T onlyoffice documentserver-generate-allfonts.sh
    "${compose[@]}" --profile editor restart onlyoffice
    wait_for_editor_health
    "${compose[@]}" --profile editor ps onlyoffice
    ;;
  editor-logs)
    "${compose[@]}" --profile editor logs -f onlyoffice
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|recreate|status|logs [service]|editor-start|editor-stop|editor-status|editor-fonts|editor-logs}" >&2
    exit 2
    ;;
esac
