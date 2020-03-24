package org.gpc4j.corona.dto;

import lombok.Data;

/**
 * Entry for a given day for a given State and the file name the
 * entry is from.
 */
@Data
public class StateDayEntry {
  private String fileName;
  private String name;
  private String symbol;
  private int cumulative;
  private int active;
  private int deaths;
}
