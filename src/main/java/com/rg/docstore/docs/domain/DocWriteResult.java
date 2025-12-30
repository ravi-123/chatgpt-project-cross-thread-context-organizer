package com.rg.docstore.docs.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocWriteResult(
    DocId docId,
    int revision,
    String updatedAt,
    String etag,

    // NEW (nullable)
    DocWriteCause cause,                 // "CREATE" | "PUT" | "APPEND" | "RESTORE"
    Integer restoredFromRevision  // only when cause == "RESTORE"
) {

  public static DocWriteResult normal(DocId docId, int revision, String updatedAt, String etag, DocWriteCause cause) {
    return new DocWriteResult(docId, revision, updatedAt, etag, cause, null);
  }

  public static DocWriteResult restored(DocId docId, int revision, String updatedAt, String etag, int restoredFrom) {
    return new DocWriteResult(docId, revision, updatedAt, etag, DocWriteCause.RESTORE, restoredFrom);
  }
}
