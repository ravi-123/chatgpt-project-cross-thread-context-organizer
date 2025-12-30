package com.rg.docstore.docs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocMetaResponse(
    String docId,
    int revision,
    String updatedAt,

    // NEW (nullable)
    String cause,
    Integer restoredFromRevision
) {}
