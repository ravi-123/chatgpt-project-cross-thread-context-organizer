package com.rg.docstore.docs.domain;

public class DocErrors {
  private DocErrors() {}
  public static class NotFound extends RuntimeException {
    public NotFound(String msg) { super(msg); }
  }

  public static class Conflict extends RuntimeException {
    public final int currentRevision;
    public Conflict(String msg, int currentRevision) {
      super(msg);
      this.currentRevision = currentRevision;
    }
  }

  public static class AlreadyExists extends RuntimeException {
    public AlreadyExists(String msg) { super(msg); }
  }

  public static class BadRequest extends RuntimeException {
    public BadRequest(String msg) { super(msg); }
  }
}
