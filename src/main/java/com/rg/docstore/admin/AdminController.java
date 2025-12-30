package com.rg.docstore.admin;

import com.rg.docstore.auth.ApiKeyStore;
import com.rg.docstore.admin.model.CreateProjectRequest;
import com.rg.docstore.admin.model.ProjectKeyResponse;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin")
public class AdminController {
  private final ApiKeyStore apiKeyStore;
  private final String adminToken;

  public AdminController(ApiKeyStore apiKeyStore, @Value("${app.adminToken}") String adminToken) {
    this.apiKeyStore = apiKeyStore;
    this.adminToken = adminToken;
  }

  @PostMapping("/projects")
  public ProjectKeyResponse createProject(@RequestHeader("X-Admin-Token") String token,
                                              @Valid @RequestBody CreateProjectRequest req) {
    if (!adminToken.equals(token)) throw new Forbidden("Invalid admin token");
    return AdminApiMapper.toResponse(apiKeyStore.createProjectKey(req.projectName()));
  }

  @ResponseStatus(code = org.springframework.http.HttpStatus.FORBIDDEN)
  static class Forbidden extends RuntimeException {
    Forbidden(String msg) { super(msg); }
  }
}
