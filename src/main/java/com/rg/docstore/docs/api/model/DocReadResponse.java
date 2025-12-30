package com.rg.docstore.docs.api.model;

public record DocReadResponse(
    String docId,
    int revision,
    String contentType,
    String content,
    String updatedAt
) {}
