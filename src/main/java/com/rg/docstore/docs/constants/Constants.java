package com.rg.docstore.docs.constants;

import java.util.Set;

public class Constants {
  public static final String DOC_ID_REGEX = "^[a-zA-Z0-9](?:[a-zA-Z0-9._-]{0,98}[a-zA-Z0-9])?$";
  public static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
      "CON","PRN","AUX","NUL",
      "COM1","COM2","COM3","COM4","COM5","COM6","COM7","COM8","COM9",
      "LPT1","LPT2","LPT3","LPT4","LPT5","LPT6","LPT7","LPT8","LPT9"
  );


  private Constants() {}
}
