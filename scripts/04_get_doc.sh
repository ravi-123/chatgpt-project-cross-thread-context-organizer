#!/usr/bin/env bash
set -euo pipefail
API_KEY="${1:?API key required}"
DOC_ID="${2:?docId required}"

curl -i -sS "http://localhost:8080/v1/docs/${DOC_ID}" \
  -H "Authorization: Bearer ${API_KEY}"
