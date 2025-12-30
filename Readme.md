# chatgpt-project-cross-thread-context-organizer

Updated: 30/12/2025 02:07:07 PM IST

A **local, versioned Markdown document store** (file-based MVP) built to act as a **single source of truth** for ChatGPT Project docs (Strategy / Tracker / Learnings / Timings), generalized to any number of documents.

This repo currently contains the **rg-docstore** Spring Boot service at the repo root.

---

## What problem this solves

ChatGPT “Canvas” (or docs inside chat threads) can be hard to keep consistent **across multiple threads**.  
`rg-docstore` provides a **canonical docstore** with:

- File-based storage (easy to swap to Postgres later)
- Full-history versions per document
- ETag + `If-Match` optimistic concurrency (prevents lost updates)
- Append + Put workflows (ideal for logs and trackers)
- Restore any prior revision (creates a new revision with `RESTORE` cause)
- Data retention policy (keep last N + time buckets via SDK)

---

## High-level architecture

- **rg-docstore (this repo / Java service)**: stores docs, versions, metadata, retention
- **MCP wrapper (recommended next step)**: exposes docstore as MCP tools so ChatGPT can read/write it from anywhere

**Flow (ideal):**  
ChatGPT → MCP tools → rg-docstore REST → filesystem (versions + latest) → retention cleanup

---

## Core concepts

### Identifiers
- **docId**: unique document id (also used as filename / logical key). No duplicates.
- **revision**: monotonically increasing version number for each doc.
- **ETag**: returned on reads; required via `If-Match` on writes to ensure safe concurrency.

### Concurrency + safety
- **Single-writer lock**: FileChannel lock inside write operations
- **Optimistic concurrency**: `If-Match` must match the current `ETag`, else server rejects with a precondition failure

### Storage layout (file I/O MVP)

```
data/<projectId>/docs/<docId>/
  latest.md                 # latest content
  versions/<rev>.md         # version history
  meta.json                 # current revision/hash/updatedAt etc.
  index.json                # revision -> metadata (editedAt, cause, restoredFromRevision, ...)
```

Write operations create a new `versions/<rev>.md`, update `latest.md`, then update JSON indexes and apply retention.

---

## API Overview (selected)

Base health/info:
- `GET /health`
- `GET /v1/info`

Docs:
- `GET /v1/docs`
- `POST /v1/docs` (create)
- `GET /v1/docs/{docId}` (read latest)
- `PUT /v1/docs/{docId}` (replace full content; supports `If-Match`)
- `POST /v1/docs/{docId}/append` (append; supports `If-Match`)
- `GET /v1/docs/{docId}/versions?limit=...`
- `GET /v1/docs/{docId}/versions/{revision}`
- `POST /v1/docs/{docId}/restore/{revision}` (supports `If-Match`, creates a new revision)

Admin (optional):
- `POST /v1/admin/projects` (requires admin token header, e.g. `X-Admin-Token`)

> Note: See the generated OpenAPI (`api-docs.json` in your local build output, if enabled) or `GET /v1/info` for current configuration details.

---

## Quickstart (local dev)

### Prereqs
- Java 17
- Gradle (wrapper included)

### Run
```bash
./gradlew bootRun
```

### Verify
```bash
curl -s http://localhost:8080/health
curl -s http://localhost:8080/v1/info
```

---

## Common workflows (curl examples)

### 1) Create a doc
```bash
curl -s -X POST http://localhost:8080/v1/docs   -H "Content-Type: application/json"   -d '{
    "docId": "DSA__Timings_Log",
    "contentType": "text/markdown",
    "content": "# DSA — Timings Log\n\nUpdated (IST): ...\n"
  }'
```

### 2) Read latest (capture ETag)
```bash
curl -i -s http://localhost:8080/v1/docs/DSA__Timings_Log
```

Look for the `ETag` header and/or `etag` field in response JSON (depending on your implementation).

### 3) Append safely (If-Match)
```bash
ETAG='"rev-12:sha256-..."'

curl -s -X POST http://localhost:8080/v1/docs/DSA__Timings_Log/append   -H "Content-Type: application/json"   -H "If-Match: $ETAG"   -d '{
    "append": "\n## 30/12/2025\n- 01:45 pm — Started: LC 239\n"
  }'
```

### 4) Replace content safely (If-Match)
```bash
ETAG='"rev-12:sha256-..."'

curl -s -X PUT http://localhost:8080/v1/docs/DSA__Timings_Log   -H "Content-Type: application/json"   -H "If-Match: $ETAG"   -d '{
    "content": "# DSA — Timings Log\n\nUpdated (IST): ...\n..."
  }'
```

### 5) List versions
```bash
curl -s "http://localhost:8080/v1/docs/DSA__Timings_Log/versions?limit=20"
```

### 6) Restore a previous revision
```bash
ETAG='"rev-12:sha256-..."'

curl -s -X POST http://localhost:8080/v1/docs/DSA__Timings_Log/restore/7   -H "If-Match: $ETAG"
```

This creates a **new revision** with cause `RESTORE` and `restoredFromRevision=7`.

---

## Retention policy (high level)

Retention is applied **after each successful write**. Policy supports:
- **Keep last N versions**
- **Time-bucket snapshots** (fixed durations and calendar periods)

Current policy is visible via:
- `GET /v1/info`

---

## Suggested next step: MCP wrapper

To make this docstore usable from ChatGPT across threads, run an MCP server that wraps these REST endpoints as tools, e.g.:

- `doc_get(doc_id)`
- `doc_put(doc_id, content, if_match)`
- `doc_append(doc_id, append, if_match)`
- `doc_versions(doc_id, limit)`
- `doc_restore(doc_id, revision, if_match)`
- plus connector-friendly `search` / `fetch`

---

## Troubleshooting

- **412 / precondition failed** on write: your `If-Match` ETag is stale → re-read latest and retry.
- **Doc not found**: create it first (POST `/v1/docs`), or verify `docId` spelling.
- **Unexpected retention deletes**: check current retention config in `/v1/info`.
- **File permission issues**: ensure the process has RW access to the data directory.

---

## License

Add your preferred license (MIT/Apache-2.0/etc.).

