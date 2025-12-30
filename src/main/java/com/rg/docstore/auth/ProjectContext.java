package com.rg.docstore.auth;

public final class ProjectContext {
  private static final ThreadLocal<String> PROJECT = new ThreadLocal<>();

  public static void set(String projectId) { PROJECT.set(projectId); }

  public static String getRequired() {
    String v = PROJECT.get();
    if (v == null) throw new IllegalStateException("No project context");
    return v;
  }

  public static void clear() { PROJECT.remove(); }

  private ProjectContext() {}
}
