package org.gpc4j.corona.raven;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CountyEntry {

  private String name;
  private String state;
  private String day;
  private int cumulative;
  private int deaths;

}
