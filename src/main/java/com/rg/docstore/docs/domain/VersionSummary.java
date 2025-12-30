package com.rg.docstore.docs.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionSummary(
    int revision,
    String hash,
    String editedAt,
    DocWriteCause cause,
    Integer restoredFromRevision
) {}
