package com.rg.docstore.docs.api.error;

import com.rg.docstore.docs.domain.DocErrors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocErrors.BadRequest.class)
    public ResponseEntity<?> badRequest(DocErrors.BadRequest e) {
        return ResponseEntity.status(400).body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(DocErrors.NotFound.class)
    public ResponseEntity<?> notFound(DocErrors.NotFound e) {
        return ResponseEntity.status(404).body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(DocErrors.AlreadyExists.class)
    public ResponseEntity<?> exists(DocErrors.AlreadyExists e) {
        return ResponseEntity.status(409).body(Map.of("error", "ALREADY_EXISTS", "message", e.getMessage()));
    }

    @ExceptionHandler(DocErrors.Conflict.class)
    public ResponseEntity<?> conflict(DocErrors.Conflict e) {
        return ResponseEntity.status(409).body(Map.of(
            "error", "ETAG_MISMATCH",
            "message", e.getMessage(),
            "currentRevision", e.currentRevision
        ));
    }
}
