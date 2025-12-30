package com.rg.docstore.docs.api;

import com.rg.docstore.docs.api.mapper.DocApiMapper;
import com.rg.docstore.docs.api.model.AppendDocRequest;
import com.rg.docstore.docs.api.model.CreateDocRequest;
import com.rg.docstore.docs.api.model.DocMetaResponse;
import com.rg.docstore.docs.api.model.DocReadResponse;
import com.rg.docstore.docs.api.model.DocSummaryResponse;
import com.rg.docstore.docs.api.model.PutDocRequest;
import com.rg.docstore.docs.api.model.VersionSummaryResponse;
import com.rg.docstore.docs.application.DocService;
import com.rg.docstore.docs.domain.Doc;
import com.rg.docstore.docs.domain.DocId;
import com.rg.docstore.docs.domain.DocWriteResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/v1/docs")
public class DocController {

  private final DocService svc;

  public DocController(DocService svc) {
    this.svc = svc;
  }

  @PostMapping
  public ResponseEntity<DocMetaResponse> create(@Valid @RequestBody CreateDocRequest req) {
    DocWriteResult r = svc.create(new DocId(req.docId()), req.contentType(), req.content());
    return ResponseEntity.status(201)
        .header(HttpHeaders.ETAG, r.etag())
        .body(DocApiMapper.toMeta(r));
  }

  @GetMapping
  public List<DocSummaryResponse> list() {
    return svc.list().stream().map(DocApiMapper::toSummary).toList();
  }

  @GetMapping("/{docId}")
  public ResponseEntity<DocReadResponse> get(@PathVariable String docId) {
    Doc d = svc.get(new DocId(docId));
    return ResponseEntity.ok()
        .header(HttpHeaders.ETAG, d.etag())
        .body(DocApiMapper.toRead(d));
  }

  @PutMapping("/{docId}")
  public ResponseEntity<DocMetaResponse> put(@PathVariable String docId,
                                             @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                             @Valid @RequestBody PutDocRequest req) {
    DocWriteResult r = svc.put(new DocId(docId), ifMatch, req.content());
    return ResponseEntity.ok()
        .header(HttpHeaders.ETAG, r.etag())
        .body(DocApiMapper.toMeta(r));
  }

  @PostMapping("/{docId}/append")
  public ResponseEntity<DocMetaResponse> append(@PathVariable String docId,
                                                @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                @Valid @RequestBody AppendDocRequest req) {
    DocWriteResult r = svc.append(new DocId(docId), ifMatch, req.append());
    return ResponseEntity.ok()
        .header(HttpHeaders.ETAG, r.etag())
        .body(DocApiMapper.toMeta(r));
  }

  @GetMapping("/{docId}/versions")
  public List<VersionSummaryResponse> versions(@PathVariable String docId,
                                               @RequestParam(defaultValue = "20") @Min(1) int limit) {
    return svc.versions(new DocId(docId), limit).stream().map(DocApiMapper::toVersion).toList();
  }

  @GetMapping("/{docId}/versions/{revision}")
  public ResponseEntity<DocReadResponse> getVersion(@PathVariable String docId,
                                                    @PathVariable @Min(1) int revision) {
    Doc d = svc.getVersion(new DocId(docId), revision);
    return ResponseEntity.ok()
        .header(HttpHeaders.ETAG, d.etag())
        .body(DocApiMapper.toRead(d));
  }

  @PostMapping("/{docId}/restore/{revision}")
  public ResponseEntity<DocMetaResponse> restore(@PathVariable String docId,
                                                 @PathVariable @Min(1) int revision,
                                                 @RequestHeader(value = "If-Match", required = false) String ifMatch) {
    DocWriteResult r = svc.restore(new DocId(docId), revision, ifMatch);
    return ResponseEntity.ok()
        .header(HttpHeaders.ETAG, r.etag())
        .body(DocApiMapper.toMeta(r));
  }
}
