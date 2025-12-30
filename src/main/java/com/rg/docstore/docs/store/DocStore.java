package com.rg.docstore.docs.store;

import com.rg.docstore.docs.domain.Doc;
import com.rg.docstore.docs.domain.DocId;
import com.rg.docstore.docs.domain.DocSummary;
import com.rg.docstore.docs.domain.DocWriteResult;
import com.rg.docstore.docs.domain.VersionSummary;

import java.util.List;
import java.util.Optional;

public interface DocStore {
  DocWriteResult create(String projectId, DocId docId, String contentType, String content);
  Optional<Doc> get(String projectId, DocId docId);
  List<DocSummary> list(String projectId);

  DocWriteResult put(String projectId, DocId docId, String ifMatchEtag, String newContent);
  DocWriteResult append(String projectId, DocId docId, String ifMatchEtag, String append);

  List<VersionSummary> listVersions(String projectId, DocId docId, int limit);
  Optional<Doc> getVersion(String projectId, DocId docId, int revision);
  DocWriteResult restore(String projectId, DocId docId, int revisionToRestore, String ifMatchEtag);
}
