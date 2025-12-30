package com.rg.docstore.docs.domain;

public record Doc(
    DocId docId,
    int revision,
    String contentType,
    String content,
    String updatedAt,
    String etag
) {}
