package com.rg.docstore.docs.api.model;

public record DocSummaryResponse(
    String docId,
    int revision,
    String updatedAt
) {}
