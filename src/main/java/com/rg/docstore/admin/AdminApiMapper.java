package com.rg.docstore.admin;

import com.rg.docstore.admin.model.ProjectKeyResponse;
import com.rg.docstore.auth.ApiKeyStore;

public final class AdminApiMapper {
  private AdminApiMapper() {}

  public static ProjectKeyResponse toResponse(ApiKeyStore.ProjectKey k) {
    return new ProjectKeyResponse(k.projectId(), k.apiKey());
  }
}