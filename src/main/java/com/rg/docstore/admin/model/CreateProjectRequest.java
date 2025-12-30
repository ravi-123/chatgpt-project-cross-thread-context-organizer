package com.rg.docstore.admin.model;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(@NotBlank String projectName) {}
