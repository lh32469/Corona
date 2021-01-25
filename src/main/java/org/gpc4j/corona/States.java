package org.gpc4j.corona;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class States {

  public static final Map<String, String> NAME_TO_SYMBOL = new HashMap<>();
  public static final Map<String, String> SYMBOL_TO_NAME = new HashMap<>();
  public static final List<String> ALL_SYMBOLS;

  static {

    NAME_TO_SYMBOL.put("Alabama", "AL");
    NAME_TO_SYMBOL.put("Alaska", "AK");
    NAME_TO_SYMBOL.put("Arizona", "AZ");
    NAME_TO_SYMBOL.put("Arkansas", "AR");
    NAME_TO_SYMBOL.put("California", "CA");
    NAME_TO_SYMBOL.put("Colorado", "CO");
    NAME_TO_SYMBOL.put("Connecticut", "CT");
    NAME_TO_SYMBOL.put("Delaware", "DE");
    NAME_TO_SYMBOL.put("Florida", "FL");
    NAME_TO_SYMBOL.put("Georgia", "GA");
    NAME_TO_SYMBOL.put("Hawaii", "HI");
    NAME_TO_SYMBOL.put("Idaho", "ID");
    NAME_TO_SYMBOL.put("Illinois", "IL");
    NAME_TO_SYMBOL.put("Indiana", "IN");
    NAME_TO_SYMBOL.put("Iowa", "IA");
    NAME_TO_SYMBOL.put("Kansas", "KS");
    NAME_TO_SYMBOL.put("Kentucky", "KY");
    NAME_TO_SYMBOL.put("Louisiana", "LA");
    NAME_TO_SYMBOL.put("Maine", "ME");
    NAME_TO_SYMBOL.put("Maryland", "MD");
    NAME_TO_SYMBOL.put("Massachusetts", "MA");
    NAME_TO_SYMBOL.put("Michigan", "MI");
    NAME_TO_SYMBOL.put("Minnesota", "MN");
    NAME_TO_SYMBOL.put("Mississippi", "MS");
    NAME_TO_SYMBOL.put("Missouri", "MO");
    NAME_TO_SYMBOL.put("Montana", "MT");
    NAME_TO_SYMBOL.put("Nebraska", "NE");
    NAME_TO_SYMBOL.put("Nevada", "NV");
    NAME_TO_SYMBOL.put("Ohio", "OH");
    NAME_TO_SYMBOL.put("Oklahoma", "OK");
    NAME_TO_SYMBOL.put("Oregon", "OR");
    NAME_TO_SYMBOL.put("Pennsylvania", "PA");
    NAME_TO_SYMBOL.put("Tennessee", "TN");
    NAME_TO_SYMBOL.put("Texas", "TX");
    NAME_TO_SYMBOL.put("Utah", "UT");
    NAME_TO_SYMBOL.put("Vermont", "VT");
    NAME_TO_SYMBOL.put("Virginia", "VA");
    NAME_TO_SYMBOL.put("Washington", "WA");
    NAME_TO_SYMBOL.put("Wisconsin", "WI");
    NAME_TO_SYMBOL.put("Wyoming", "WY");

    NAME_TO_SYMBOL.put("New Hampshire", "NH");
    NAME_TO_SYMBOL.put("New Jersey", "NJ");
    NAME_TO_SYMBOL.put("New Mexico", "NM");
    NAME_TO_SYMBOL.put("New York", "NY");
    NAME_TO_SYMBOL.put("North Carolina", "NC");
    NAME_TO_SYMBOL.put("North Dakota", "ND");
    NAME_TO_SYMBOL.put("Rhode Island", "RI");
    NAME_TO_SYMBOL.put("South Carolina", "SC");
    NAME_TO_SYMBOL.put("South Dakota", "SD");
    NAME_TO_SYMBOL.put("West Virginia", "WV");
    NAME_TO_SYMBOL.put("District of Columbia", "DC");

    // Create reverse mapping of symbol to name
    for (String key : NAME_TO_SYMBOL.keySet()) {
      SYMBOL_TO_NAME.put(NAME_TO_SYMBOL.get(key), key);
    }


    ALL_SYMBOLS = Collections
        .unmodifiableList(new LinkedList<>(NAME_TO_SYMBOL.values()));

  }


}
