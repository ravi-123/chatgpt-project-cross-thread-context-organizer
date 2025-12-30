#!/usr/bin/env bash
set -euo pipefail
ADMIN_TOKEN="${1:-CHANGE_ME_ADMIN_TOKEN}"
PROJECT_NAME="${2:-DSA Project 1}"

curl -sS -X POST "http://localhost:8080/v1/admin/projects" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: ${ADMIN_TOKEN}" \
  -d "{\"projectName\":\"${PROJECT_NAME}\"}" | jq .
