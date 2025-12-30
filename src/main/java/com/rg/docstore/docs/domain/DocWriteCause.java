package com.rg.docstore.docs.domain;

public enum DocWriteCause {
  CREATE,
  PUT,
  APPEND,
  RESTORE;

  public static DocWriteCause fromString(String s) {
    if (s == null) return null;
    try {
      return DocWriteCause.valueOf(s.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
