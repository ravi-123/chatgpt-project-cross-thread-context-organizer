# rg-docstore (Markdown Doc Store)

A **project-scoped Markdown document store** (Spring Boot 3.5.0) that supports:
- File-system storage (swap-friendly storage layer)
- Doc versioning + restore
- Optimistic concurrency using **ETag + If-Match**
- Multi-project isolation (**Option A**): project is derived from API key, clients never send `projectId`

---

## Why this exists (use-case)
This service is designed to act as a “single source of truth” store for documents (e.g., DSA progress logs, learnings, trackers),
where an LLM agent (ChatGPT) can:
1) fetch a doc,
2) apply an edit,
3) write back safely using `If-Match` to prevent accidental overwrites.

---

## Tech
- Java 17
- Spring Boot 3.5.0
- Gradle (Groovy DSL)
- Storage: File I/O with atomic writes + simple lock

---

## Quick Start

### 1) Configure
Edit `src/main/resources/application.yml`:

```yaml
app:
  dataDir: ./data
  adminToken: "CHANGE_ME_ADMIN_TOKEN"
