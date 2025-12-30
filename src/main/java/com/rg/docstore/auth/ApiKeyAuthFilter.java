package com.rg.docstore.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
  private final ApiKeyStore apiKeyStore;

  public ApiKeyAuthFilter(ApiKeyStore apiKeyStore) {
    this.apiKeyStore = apiKeyStore;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String p = request.getRequestURI();
    return p.startsWith("/v1/admin")
        || p.equals("/")
        || p.equals("/v1/info")
        || p.equals("/health")
        || p.equals("/v3/api-docs")
        || p.startsWith("/error");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth == null || !auth.startsWith("Bearer ")) {
      response.sendError(401, "Missing Authorization: Bearer <apiKey>");
      return;
    }

    String apiKey = auth.substring("Bearer ".length()).trim();
    apiKeyStore.resolveProjectId(apiKey).ifPresentOrElse(projectId -> {
      try {
        ProjectContext.set(projectId);
        filterChain.doFilter(request, response);
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        ProjectContext.clear();
      }
    }, () -> {
      try { response.sendError(401, "Invalid API key"); } catch (IOException ignored) {}
    });
  }
}
