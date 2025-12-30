package com.rg.docstore.docs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rg.docstore.docs.domain.DocWriteCause;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionSummaryResponse(
    int revision,
    String hash,
    String editedAt,
    DocWriteCause cause,
    Integer restoredFromRevision
) {}
