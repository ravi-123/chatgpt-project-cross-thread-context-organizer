package com.rg.docstore.docs.domain;

import java.util.Locale;

import static com.rg.docstore.docs.constants.Constants.DOC_ID_REGEX;
import static com.rg.docstore.docs.constants.Constants.WINDOWS_RESERVED_NAMES;

public record DocId(String value) {

  public DocId {
    if (value == null || !value.matches(DOC_ID_REGEX)) {
      throw new DocErrors.BadRequest("Invalid docId. Allowed: " + DOC_ID_REGEX);
    }
    if (WINDOWS_RESERVED_NAMES.contains(value.toUpperCase(Locale.ROOT))) {
      throw new DocErrors.BadRequest("Invalid docId: reserved filename on Windows: " + value);
    }
  }

  @Override public String toString() { return value; }
}
