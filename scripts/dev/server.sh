#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../.." && pwd)"
server_root="${project_root}/apps/docmind-server"

usage() {
  echo "Usage: $0 {dev|test|verify|clean}"
}

cd "${server_root}"

case "${1:-}" in
  dev)
    exec ./mvnw spring-boot:run
    ;;
  test|verify|clean)
    exec ./mvnw "$1"
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
