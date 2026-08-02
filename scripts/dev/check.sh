#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"${script_dir}/web.sh" check
"${script_dir}/document-ai.sh" check
"${script_dir}/server.sh" verify
