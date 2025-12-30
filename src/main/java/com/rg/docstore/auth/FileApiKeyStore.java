package com.rg.docstore.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Component
public class FileApiKeyStore implements ApiKeyStore {
  private final Path keysFile;
  private final ObjectMapper om = new ObjectMapper();

  public FileApiKeyStore(@Value("${app.dataDir}") String dataDir) throws IOException {
    Path dir = Paths.get(dataDir).resolve("keys");
    Files.createDirectories(dir);
    this.keysFile = dir.resolve("keys.json");
    if (!Files.exists(keysFile)) {
      Files.writeString(keysFile, "{}", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }
  }

  @Override
  public Optional<String> resolveProjectId(String rawApiKey) {
    try {
      String keyHash = sha256Hex(rawApiKey);
      Map<String, KeyRecord> map = readAll();
      KeyRecord rec = map.get(keyHash);
      if (rec == null || rec.revokedAt != null) return Optional.empty();
      return Optional.of(rec.projectId);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public ProjectKey createProjectKey(String projectName) {
    try {
      String projectId = UUID.randomUUID().toString();
      String apiKey = "rk_" + UUID.randomUUID().toString().replace("-", "");
      String keyHash = sha256Hex(apiKey);

      Map<String, KeyRecord> map = readAll();
      while (map.containsKey(keyHash)) {
        apiKey = "rk_" + UUID.randomUUID().toString().replace("-", "");
        keyHash = sha256Hex(apiKey);
      }

      map.put(keyHash, new KeyRecord(projectId, projectName, Instant.now().toString(), null));
      writeAll(map);

      return new ProjectKey(projectId, apiKey);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create project key", e);
    }
  }

  private Map<String, KeyRecord> readAll() throws IOException {
    String json = Files.readString(keysFile, StandardCharsets.UTF_8).trim();
    if (json.isEmpty()) json = "{}";
    return om.readValue(json, new TypeReference<Map<String, KeyRecord>>() {});
  }

  private void writeAll(Map<String, KeyRecord> map) throws IOException {
    Path tmp = keysFile.resolveSibling("keys.json.tmp");
    Files.writeString(
        tmp,
        om.writerWithDefaultPrettyPrinter().writeValueAsString(map),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    );
    Files.move(tmp, keysFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private static String sha256Hex(String s) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder(out.length * 2);
    for (byte b : out) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  private static class KeyRecord {
    public String projectId;
    public String projectName;
    public String createdAt;
    public String revokedAt;

    public KeyRecord() {}
    public KeyRecord(String projectId, String projectName, String createdAt, String revokedAt) {
      this.projectId = projectId;
      this.projectName = projectName;
      this.createdAt = createdAt;
      this.revokedAt = revokedAt;
    }
  }
}
