package com.rg.docstore.meta.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class AppErrorController implements ErrorController {

  @RequestMapping("/error")
  public Map<String, Object> handleError(HttpServletRequest request) {
    Object sc = request.getAttribute("jakarta.servlet.error.status_code");
    int statusCode = (sc instanceof Integer i) ? i : 500;

    Object msg = request.getAttribute("jakarta.servlet.error.message");
    Object uri = request.getAttribute("jakarta.servlet.error.request_uri");

    HttpStatus status = HttpStatus.resolve(statusCode);
    String statusText = (status != null) ? status.getReasonPhrase() : "Unknown";

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("timeUtc", Instant.now().toString());
    out.put("status", statusCode);
    out.put("error", statusText);
    out.put("path", uri);
    out.put("message", msg);

    out.put("hints", Map.of(
        "publicEndpoints", "GET /, GET /v1/info, GET /health",
        "docsAuth", "For /v1/docs/** use header: Authorization: Bearer <PROJECT_API_KEY>",
        "adminAuth", "For /v1/admin/** use header: X-Admin-Token: <ADMIN_TOKEN>"
    ));

    return out;
  }
}
