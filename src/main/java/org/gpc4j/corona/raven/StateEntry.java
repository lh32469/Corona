package org.gpc4j.corona.raven;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class StateEntry {

  private String day;
  private Map<String, CountyEntry> counties = new HashMap<>();

  /**
   * Full State name.
   */
  private String state;

  /**
   * Abbreviated, two letter symbol.
   */
  private String symbol;
  private int cumulative;
  private int deaths;

  /**
   * Update State counts based on County information.
   */
  public void update() {
    cumulative = counties.values().stream()
        .mapToInt(CountyEntry::getCumulative)
        .sum();
    deaths = counties.values().stream()
        .mapToInt(CountyEntry::getDeaths)
        .sum();
  }
}
