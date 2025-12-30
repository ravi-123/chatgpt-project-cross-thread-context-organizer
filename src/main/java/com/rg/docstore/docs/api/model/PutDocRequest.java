package com.rg.docstore.docs.api.model;

import jakarta.validation.constraints.NotNull;

public record PutDocRequest(@NotNull String content) {}
