package com.rg.docstore.docs.store.filesystem;

import com.fasterxml.jackson.annotation.JsonInclude;

public class Models {
  private Models() {}
  public record Meta(String docId, String contentType, int currentRevision, String contentHash, String updatedAt) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record VersionMeta(
      int revision,
      String hash,
      String editedAt,
      String cause,                 // store as String to avoid coupling store to domain enum
      Integer restoredFromRevision  // nullable
  ) {}
}
