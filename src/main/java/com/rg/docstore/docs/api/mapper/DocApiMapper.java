package com.rg.docstore.docs.api.mapper;

import com.rg.docstore.docs.api.model.*;
import com.rg.docstore.docs.domain.*;

public final class DocApiMapper {
  private DocApiMapper() {}

  public static DocReadResponse toRead(Doc d) {
    return new DocReadResponse(d.docId().value(), d.revision(), d.contentType(), d.content(), d.updatedAt());
  }

  public static DocMetaResponse toMeta(DocWriteResult r) {
    return new DocMetaResponse(
        r.docId().value(),
        r.revision(),
        r.updatedAt(),
        r.cause().name(),
        r.restoredFromRevision()
    );
  }

  public static DocSummaryResponse toSummary(DocSummary s) {
    return new DocSummaryResponse(s.docId().value(), s.revision(), s.updatedAt());
  }

  public static VersionSummaryResponse toVersion(VersionSummary v) {
    return new VersionSummaryResponse(v.revision(), v.hash(),
        v.editedAt(),
        v.cause(),
        v.restoredFromRevision()
    );
  }
}
