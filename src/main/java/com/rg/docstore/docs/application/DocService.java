package com.rg.docstore.docs.application;

import com.rg.docstore.auth.ProjectContext;
import com.rg.docstore.docs.domain.Doc;
import com.rg.docstore.docs.domain.DocErrors;
import com.rg.docstore.docs.domain.DocId;
import com.rg.docstore.docs.domain.DocSummary;
import com.rg.docstore.docs.domain.DocWriteResult;
import com.rg.docstore.docs.domain.VersionSummary;
import com.rg.docstore.docs.store.DocStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocService {

  private final DocStore store;

  public DocService(DocStore store) {
    this.store = store;
  }

  private String pid() {
    return ProjectContext.getRequired();
  }

  public DocWriteResult create(DocId docId, String contentType, String content) {
    return store.create(pid(), docId, contentType, content);
  }

  public Doc get(DocId docId) {
    return store.get(pid(), docId)
        .orElseThrow(() -> new DocErrors.NotFound("Doc not found: " + docId.value()));
  }

  public List<DocSummary> list() {
    return store.list(pid());
  }

  public DocWriteResult put(DocId docId, String ifMatchEtag, String newContent) {
    return store.put(pid(), docId, ifMatchEtag, newContent);
  }

  public DocWriteResult append(DocId docId, String ifMatchEtag, String append) {
    return store.append(pid(), docId, ifMatchEtag, append);
  }

  public List<VersionSummary> versions(DocId docId, int limit) {
    return store.listVersions(pid(), docId, limit);
  }

  public Doc getVersion(DocId docId, int revision) {
    return store.getVersion(pid(), docId, revision)
        .orElseThrow(() -> new DocErrors.NotFound("Version not found: docId=" + docId.value() + ", revision=" + revision));
  }

  public DocWriteResult restore(DocId docId, int revisionToRestore, String ifMatchEtag) {
    return store.restore(pid(), docId, revisionToRestore, ifMatchEtag);
  }
}
