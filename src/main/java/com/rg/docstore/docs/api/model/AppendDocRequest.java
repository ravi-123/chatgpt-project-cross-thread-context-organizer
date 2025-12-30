package com.rg.docstore.docs.api.model;

import jakarta.validation.constraints.NotBlank;

public record AppendDocRequest(@NotBlank String append) {}
