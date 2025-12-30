package com.rg.docstore.meta.api;

import static com.rg.docstore.docs.constants.Constants.DOC_ID_REGEX;

import com.rg.retention.RetentionPolicy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {
  // Keep these in sync with FileSystemDocStore + DocController validation
  private static final String AUTH_HEADER = "Authorization: Bearer <PROJECT_API_KEY>";
  private static final String ADMIN_HEADER = "X-Admin-Token: <ADMIN_TOKEN>";

  @Autowired
  private RetentionPolicy retentionPolicy;

  @GetMapping("/")
  public Map<String, Object> rootInfo() {
    return info();
  }

  @GetMapping("/v1/info")
  public Map<String, Object> rootInfo2() {
    return info();
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    // Keep it dead-simple for load balancers / uptime checks
    return Map.of(
        "status", "UP",
        "timeUtc", Instant.now().toString()
    );
  }

  public Map<String, Object> info() {
    Map<String, Object> out = new LinkedHashMap<>();

    out.put("name", "rg-docstore11");
    out.put("description", "Project-scoped Markdown document store with File I/O storage, versioning, and ETag concurrency control.");
    out.put("timeUtc", Instant.now().toString());

    out.put("auth", Map.of(
        "projectScoping", "Option A: projectId is derived from project API key; clients never send projectId.",
        "projectAuthHeader", AUTH_HEADER,
        "adminHeader", ADMIN_HEADER
    ));

    out.put("docIdRules", Map.of(
        "docIdMeaning", "docId is the document name and unique key within a project.",
        "regex", DOC_ID_REGEX,
        "notes", "Only letters, digits, dot(.), underscore(_), hyphen(-). Max length 100."
    ));

    out.put("storage", Map.of(
        "engine", "filesystem",
        "layoutExample", "data/projects/<projectId>/docs/<docId>/{meta.json,<docId>.md,versions/{000001.md,index.json}}",
        "versioning", "Every write creates a new revision file and updates versions/index.json with editedAt + hash."
    ));

    out.put("concurrency", Map.of(
        "etagFormat", "W/\"<revision>:<hashPrefix>\"",
        "writeHeader", "If-Match: <ETag from latest GET>",
        "behaviorOnMismatch", "409 ETAG_MISMATCH with currentRevision"
    ));

    out.put("endpoints", Map.of(
        "admin", Map.of(
            "createProject", "POST /v1/admin/projects (header: " + ADMIN_HEADER + ")"
        ),
        "docs", Map.of(
            "create", "POST /v1/docs",
            "list", "GET /v1/docs",
            "get", "GET /v1/docs/{docId}",
            "put", "PUT /v1/docs/{docId} (If-Match required)",
            "append", "POST /v1/docs/{docId}/append (If-Match required)",
            "versions", "GET /v1/docs/{docId}/versions",
            "getVersion", "GET /v1/docs/{docId}/versions/{revision}",
            "restore", "POST /v1/docs/{docId}/restore/{revision} (If-Match required)"
        )
    ));

    out.put("retentionpolicy", Map.of(
        "lastNVersions", retentionPolicy.keepLastNVersions(),
        "bucketZone", retentionPolicy.bucketZone(),
        "tiers", retentionPolicy.tiers() == null? "0" : retentionPolicy.tiers().size()
    ));

    return out;
  }


}
