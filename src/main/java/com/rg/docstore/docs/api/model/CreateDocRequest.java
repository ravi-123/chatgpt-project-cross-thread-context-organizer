package com.rg.docstore.docs.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import static com.rg.docstore.docs.constants.Constants.DOC_ID_REGEX;

public record CreateDocRequest(
    @NotBlank
    @Pattern(regexp = DOC_ID_REGEX, message = "docId must match " + DOC_ID_REGEX)
    String docId,

    @NotBlank
    String contentType,

    @NotNull
    String content
) {}
