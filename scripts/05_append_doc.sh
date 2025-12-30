#!/usr/bin/env bash
set -euo pipefail
API_KEY="${1:?API key required}"
DOC_ID="${2:?docId required}"
IF_MATCH="${3:?If-Match (ETag) required}"
APPEND_TEXT="${4:?append text required}"

curl -i -sS -X POST "http://localhost:8080/v1/docs/${DOC_ID}/append" \
  -H "Authorization: Bearer ${API_KEY}" \
  -H "Content-Type: application/json" \
  -H "If-Match: ${IF_MATCH}" \
  -d "$(jq -n --arg a "$APPEND_TEXT" '{append:$a}')"
