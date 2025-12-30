package com.rg.docstore.docs.store.filesystem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.rg.docstore.docs.domain.Doc;
import com.rg.docstore.docs.domain.DocErrors.AlreadyExists;
import com.rg.docstore.docs.domain.DocErrors.Conflict;
import com.rg.docstore.docs.domain.DocErrors.NotFound;
import com.rg.docstore.docs.domain.DocSummary;
import com.rg.docstore.docs.domain.DocWriteCause;
import com.rg.docstore.docs.domain.VersionSummary;
import com.rg.docstore.docs.store.filesystem.Models.Meta;
import com.rg.docstore.docs.store.filesystem.Models.VersionMeta;
import com.rg.docstore.docs.domain.DocId;
import com.rg.docstore.docs.domain.DocWriteResult;
import com.rg.docstore.docs.store.DocStore;
import com.rg.retention.RetentionDecision;
import com.rg.retention.RetentionEngine;
import com.rg.retention.RetentionPolicy;
import com.rg.retention.VersionRef;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Component
public class FileSystemDocStore implements DocStore {

  public static final String VERSIONS = "versions";
  public static final String META_JSON = "meta.json";
  public static final String INDEX_JSON = "index.json";
  private final Path baseDir;

  private ObjectMapper om;
  private RetentionPolicy retentionPolicy;

  @Autowired
  public FileSystemDocStore(@Value("${app.dataDir}") String dataDir,
                            ObjectMapper om,
                            RetentionPolicy retentionPolicy) throws IOException {
    this.baseDir = Paths.get(dataDir).resolve("projects");
    Files.createDirectories(baseDir);
    this.om = om;
    this.retentionPolicy = retentionPolicy;
  }

  @Override
  public DocWriteResult create(String projectId, DocId docId, String contentType, String content) {

    try {
      Path docDir = docDir(projectId, docId);
      if (Files.exists(docDir)) throw new AlreadyExists("Doc already exists: " + docId);

      Files.createDirectories(docDir.resolve(VERSIONS));

      int rev = 1;
      String hash = sha256Hex(content);
      String now = Instant.now().toString();

      Path latest = latestFile(docDir, docId);
      writeStringAtomic(latest, content);
      writeStringAtomic(versionFile(docDir, rev), content);

      Meta meta = new Meta(docId.value(), contentType, rev, hash, now);
      writeJsonAtomic(docDir.resolve(META_JSON), meta);

      Map<Integer, VersionMeta> index = new LinkedHashMap<>();
      index.put(rev, new VersionMeta(rev, hash, now, DocWriteCause.CREATE.name(), null));
      writeIndexAtomic(docDir, index);

      return DocWriteResult.normal(docId, rev, now, etag(rev, hash), DocWriteCause.CREATE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<Doc> get(String projectId, DocId docId) {

    try {
      Path docDir = docDir(projectId, docId);
      Path metaFile = docDir.resolve(META_JSON);
      if (!Files.exists(metaFile)) return Optional.empty();

      Meta meta = readMeta(metaFile);
      Path latest = latestFile(docDir, docId);
      String content = Files.readString(latest, StandardCharsets.UTF_8);

      String etag = etag(meta.currentRevision(), meta.contentHash());
      return Optional.of(new Doc(docId, meta.currentRevision(), meta.contentType(), content, meta.updatedAt(), etag));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<DocSummary> list(String projectId) {
    Path proj = baseDir.resolve(projectId).resolve("docs");
    if (!Files.exists(proj)) return List.of();

    try (Stream<Path> stream = Files.list(proj)) {
      List<DocSummary> out = new ArrayList<>();
      stream.filter(Files::isDirectory).forEach(docDir -> {
        Path metaFile = docDir.resolve(META_JSON);
        if (!Files.exists(metaFile)) return;
        try {
          Meta meta = readMeta(metaFile);
          out.add(new DocSummary(new DocId(meta.docId()), meta.currentRevision(), meta.updatedAt()));
        } catch (IOException ignored) {}
      });
      out.sort(Comparator.comparing(doc -> doc.docId().value()));
      return out;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DocWriteResult put(String projectId, DocId docId, String ifMatchEtag, String newContent) {
    return update(projectId, docId, ifMatchEtag, (old) -> newContent, WriteContext.normal(DocWriteCause.PUT));
  }

  @Override
  public DocWriteResult append(String projectId, DocId docId, String ifMatchEtag, String append) {
    return update(projectId, docId, ifMatchEtag, (old) -> old + append, WriteContext.normal(DocWriteCause.APPEND));
  }

  @Override
  public List<VersionSummary> listVersions(String projectId, DocId docId, int limit) {

    Path docDir = docDir(projectId, docId);
    if (!Files.exists(docDir)) throw new NotFound("Doc not found: " + docId);

    Map<Integer, VersionMeta> index = readIndexOrEmpty(docDir);

    List<VersionSummary> versions = new ArrayList<>();
    for (var e : index.entrySet()) {
      versions.add(new VersionSummary(e.getKey(), e.getValue().hash(), e.getValue().editedAt(),
          DocWriteCause.fromString(e.getValue().cause()), e.getValue().restoredFromRevision()));
    }
    versions.sort(Comparator.comparingInt(VersionSummary::revision).reversed());

    if (limit > 0 && versions.size() > limit) return versions.subList(0, limit);
    return versions;
  }

  @Override
  public Optional<Doc> getVersion(String projectId, DocId docId, int revision) {

    try {
      Path docDir = docDir(projectId, docId);
      Path metaFile = docDir.resolve(META_JSON);
      if (!Files.exists(metaFile)) return Optional.empty();

      Meta meta = readMeta(metaFile);
      Path vf = versionFile(docDir, revision);
      if (!Files.exists(vf)) return Optional.empty();

      String content = Files.readString(vf, StandardCharsets.UTF_8);

      Map<Integer, VersionMeta> index = readIndexOrEmpty(docDir);
      VersionMeta vm = index.get(revision);

      String hash = (vm != null) ? vm.hash() : sha256Hex(content);
      String editedAt = (vm != null) ? vm.editedAt() : Files.getLastModifiedTime(vf).toInstant().toString();

      return Optional.of(new Doc(docId, revision, meta.contentType(), content, editedAt, etag(revision, hash)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DocWriteResult restore(String projectId, DocId docId, int revisionToRestore, String ifMatchEtag) {
    Doc ver = getVersion(projectId, docId, revisionToRestore)
        .orElseThrow(() -> new NotFound("Version not found: " + revisionToRestore));
    return update(projectId, docId, ifMatchEtag, (old) -> ver.content(), WriteContext.restoreFrom(ver.revision()));
  }

  // ========== internal ==========

  private DocWriteResult update(String projectId, DocId docId, String ifMatchEtag,
                                ContentTransform transform, WriteContext writeContext) {
    Path docDir = docDir(projectId, docId);
    Path metaFile = docDir.resolve(META_JSON);
    if (!Files.exists(metaFile)) throw new NotFound("Doc not found: " + docId);

    try {
      // single-writer lock (good enough for local MVP; Postgres later will handle tx)
      Path lock = docDir.resolve(".lock");
      try (var channel = FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
        try (var ignored = channel.lock()) {

          Meta meta = readMeta(metaFile);
          validateIfMatch(ifMatchEtag, meta.currentRevision(), meta.contentHash());

          Path latest = latestFile(docDir, docId);
          String oldContent = Files.readString(latest, StandardCharsets.UTF_8);
          String newContent = transform.apply(oldContent);

          int newRev = meta.currentRevision() + 1;
          String newHash = sha256Hex(newContent);
          String now = Instant.now().toString();

          // write latest + version
          writeStringAtomic(versionFile(docDir, newRev), newContent);
          writeStringAtomic(latest, newContent);

          // update index.json
          Map<Integer, VersionMeta> index = readIndexOrEmpty(docDir);
          index.put(newRev, new VersionMeta(newRev, newHash, now, writeContext.cause().name(), writeContext.restoredFromRevision()));
          writeIndexAtomic(docDir, index);

          // apply retention
          applyRetention(docDir, index, newRev);
          writeIndexAtomic(docDir, index);

          // update meta.json
          Meta newMeta = new Meta(docId.value(), meta.contentType(), newRev, newHash, now);
          writeJsonAtomic(metaFile, newMeta);

          if(writeContext.cause() == DocWriteCause.RESTORE) {
            return DocWriteResult.restored(docId, newRev, now, etag(newRev, newHash), writeContext.restoredFromRevision());
          }
          return DocWriteResult.normal(docId, newRev, now, etag(newRev, newHash), writeContext.cause());
        }
      }
    } catch (Conflict c) {
      throw c;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void applyRetention(Path docDir,
                              Map<Integer, VersionMeta> index,
                              int currentRevision) {

    List<VersionRef> versions = new ArrayList<>(index.size());
    for (VersionMeta vm : index.values()) {
      if (vm == null || vm.editedAt() == null) continue;
      try {
        Instant t = Instant.parse(vm.editedAt()); // your file should already store UTC ISO strings
        int rev = vm.revision();
        versions.add(new VersionRef() {
          @Override public long versionNumber() { return rev; }
          @Override public Instant timeUtc() { return t; }
        });
      } catch (Exception ignore) {
        // skip bad timestamps (or throw if you prefer strict)
      }
    }

    RetentionDecision decision = RetentionEngine.decide(versions, retentionPolicy, currentRevision);

    System.out.println("applyRetention decision keep:"+decision.keep()+" delete:"+decision.delete());

    for (Long vNum : decision.delete()) {
      int rev = Math.toIntExact(vNum);

      try {
        // delete the content file for that revision
        Files.deleteIfExists(versionFile(docDir, rev));
        // also remove it from index
        index.remove(rev);
      } catch (Exception e) {
        // best-effort: if delete failed, keep index entry
        // (or log warning)
      }
    }
  }

  private void validateIfMatch(String ifMatch, int currentRev, String currentHash) {
    if (ifMatch == null || ifMatch.isBlank()) {
      throw new Conflict("Missing If-Match", currentRev);
    }
    ParsedEtag p = ParsedEtag.parse(ifMatch);
    if (p == null) throw new Conflict("Invalid If-Match format", currentRev);

    boolean revOk = p.revision == currentRev;
    boolean hashOk = currentHash.startsWith(p.hashPrefix);
    if (!revOk || !hashOk) {
      throw new Conflict("ETag mismatch", currentRev);
    }
  }

  private Path docDir(String projectId, DocId docId) {
    return baseDir.resolve(projectId).resolve("docs").resolve(docId.value());
  }

  private Path latestFile(Path docDir, DocId docId) {
    return docDir.resolve(docId + ".md");
  }

  private Path versionFile(Path docDir, int revision) {
    return docDir.resolve(VERSIONS).resolve(String.format("%06d.md", revision));
  }

  private Path indexFile(Path docDir) {
    return docDir.resolve(VERSIONS).resolve(INDEX_JSON);
  }

  private Meta readMeta(Path metaFile) throws IOException {
    return om.readValue(Files.readString(metaFile, StandardCharsets.UTF_8), Meta.class);
  }

  private Map<Integer, VersionMeta> readIndexOrEmpty(Path docDir) {
    Path idx = indexFile(docDir);
    if (!Files.exists(idx)) return new LinkedHashMap<>();
    try {
      String json = Files.readString(idx, StandardCharsets.UTF_8);
      Map<String, VersionMeta> raw = om.readValue(json, new TypeReference<Map<String, VersionMeta>>() {});
      if (raw == null) return new HashMap<>();
      Map<Integer, VersionMeta> out = new LinkedHashMap<>();
      raw.entrySet().stream()
          .sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getKey())))
          .forEach(e -> out.put(Integer.parseInt(e.getKey()), e.getValue()));
      return out;
    } catch (Exception e) {
      return new LinkedHashMap<>();
    }
  }

  private void writeIndexAtomic(Path docDir, Map<Integer, VersionMeta> index) throws IOException {
    // JSON object keys must be strings; convert
    Map<String, VersionMeta> out = new LinkedHashMap<>();
    index.entrySet().stream()
        .sorted(Comparator.comparingInt(Map.Entry::getKey))
        .forEach(e -> out.put(String.valueOf(e.getKey()), e.getValue()));

    writeJsonAtomic(indexFile(docDir), out);
  }

  private void writeJsonAtomic(Path file, Object obj) throws IOException {
    Files.createDirectories(file.getParent());
    Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
    Files.writeString(
        tmp,
        om.writerWithDefaultPrettyPrinter().writeValueAsString(obj),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    );
    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private void writeStringAtomic(Path file, String content) throws IOException {
    Files.createDirectories(file.getParent());
    Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
    Files.writeString(tmp, content, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private static String etag(int revision, String hash) {
    String prefix = hash.length() > 12 ? hash.substring(0, 12) : hash;
    return "W/\"" + revision + ":" + prefix + "\"";
  }

  private static String sha256Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(out.length * 2);
      for (byte b : out) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private interface ContentTransform { String apply(String oldContent); }

  private record WriteContext(DocWriteCause cause, Integer restoredFromRevision) {
    static WriteContext normal(DocWriteCause c) { return new WriteContext(c, null); }
    static WriteContext restoreFrom(int fromRev) { return new WriteContext(DocWriteCause.RESTORE, fromRev); }
  }

  private static class ParsedEtag {
    final int revision;
    final String hashPrefix;

    private ParsedEtag(int revision, String hashPrefix) {
      this.revision = revision;
      this.hashPrefix = hashPrefix;
    }

    static ParsedEtag parse(String ifMatch) {
      String s = ifMatch.trim();
      if (s.startsWith("W/")) s = s.substring(2).trim();
      if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
      int idx = s.indexOf(':');
      if (idx <= 0) return null;
      try {
        int rev = Integer.parseInt(s.substring(0, idx));
        String hp = s.substring(idx + 1);
        if (hp.isBlank()) return null;
        return new ParsedEtag(rev, hp);
      } catch (Exception e) {
        return null;
      }
    }
  }
}
