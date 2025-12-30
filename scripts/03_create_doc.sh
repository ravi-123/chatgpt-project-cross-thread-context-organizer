#!/usr/bin/env bash
set -euo pipefail
API_KEY="${1:?API key required}"
DOC_ID="${2:?docId required}"
CONTENT_FILE="${3:?markdown file required}"

curl -i -sS -X POST "http://localhost:8080/v1/docs" \
  -H "Authorization: Bearer ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg docId "$DOC_ID" --arg ct "text/markdown" --arg content "$(cat "$CONTENT_FILE")" \
        '{docId:$docId, contentType:$ct, content:$content}')"
