package com.rg.docstore.auth;

import java.util.Optional;

public interface ApiKeyStore {
  Optional<String> resolveProjectId(String rawApiKey);
  ProjectKey createProjectKey(String projectName);

  record ProjectKey(String projectId, String apiKey) {}
}
