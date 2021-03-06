package org.gpc4j.corona.beans;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.gpc4j.corona.States;
import org.gpc4j.corona.dto.StateDayEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Named
public class DataBean implements Serializable {

  /**
   * Population for each State.
   */
  private ArrayNode statePopulation;

  /**
   * List of data for each state for each day.
   */
  private List<StateDayEntry> entries;

  /**
   * Location of cloned COVID-19 Git data repository.
   */
  @Value("${corona.data.repo}")
  String repoDir;

  /**
   * Number of Days to display in charts.
   */
  @Value("${corona.days}")
  int numDays;

  static Logger LOG = LoggerFactory.getLogger(DataBean.class);


  /**
   * See if Record matches the State name provided in either
   * CSVRecord format.
   */
  Predicate<CSVRecord> isState(String state) {
    final String key = "Province_State";

    return record -> {
      try {
        if (record.get(key) != null) {
          return record.get(key).equals(state);
        } else {
          return false;
        }
      } catch (IllegalArgumentException ex) {
        LOG.warn(ex.toString());
        return false;
      }

    };
  }
//FIPS,Admin2,Province_State,Country_Region,Last_Update,Lat,Long_,Confirmed,Deaths,Recovered,Active,Combined_Key,Incident_Rate,Case_Fatality_Ratio
  /**
   * Get the total number of cumulative cases from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getCumulativeCount =
      record -> {
        try {
          return Integer.parseInt(record.get("Confirmed"));
        } catch (NumberFormatException ex) {
          return 0;
        }
      };


  /**
   * Get the total number of deaths from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getDeathsCount =
      record -> {
        try {
          return Integer.parseInt(record.get("Deaths"));
        } catch (NumberFormatException ex) {
          return 0;
        }
      };

  /**
   * Get the total number of recovered cases from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getRecovered =
      record -> {
        try {
          return Integer.parseInt(record.get("Recovered"));
        } catch (NumberFormatException ex) {
          return 0;
        }
      };

  /**
   * Get the number of currently active cases from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getActiveCount =
      record -> {
        int confirmed = 0;
        int deaths = 0;
        int recovered = 0;

        try {
          confirmed = Integer.parseInt(record.get("Confirmed"));
        } catch (NumberFormatException ex) {
          LOG.info(record.getRecordNumber() + ", " + ex);
        }

        try {
          deaths = Integer.parseInt(record.get("Deaths"));
        } catch (NumberFormatException ex) {
          LOG.info(record.getRecordNumber() + ", " + ex);
        }

        try {
          recovered = Integer.parseInt(record.get("Recovered"));
        } catch (NumberFormatException ex) {
          LOG.info(record.getRecordNumber() + ", " + ex);
        }

        return confirmed - deaths - recovered;
      };

  public Stream<StateDayEntry> getEntries() {
    return entries.parallelStream();
  }

  public StateDayEntry getEntry(final String day, final String state) {
    return entries.stream()
        .filter(e -> e.getFileName().equals(day))
        .filter(e -> e.getName().equals(state))
        .findAny().get();
  }

  public Set<String> getSortedDays() {

    // TODO: Make this a field entry calculated once.

    Set<String> days = entries.parallelStream()
        .map(entry -> entry.getFileName())
        .collect(Collectors.toSet());

    // return sorted results
    return new TreeSet<>(days);
  }

  @PostConstruct
  public void postConstruct() {
    LOG.info("postConstruct");

    try {
      entries = loadData();

      // Load State population data
      statePopulation = loadStatePopulationData();

    } catch (IOException e) {
      LOG.error(e.toString(), e);
      return;
    }

  }

  /**
   * Get the population for the State provided.
   *
   * @param state Examples: Michigan, Oregon
   */
  public int getPopulation(final String state) {

    return StreamSupport.stream(statePopulation.spliterator(), false)
        .filter(s -> s.get("State").asText().equals(state))
        .map(s -> s.get("Pop2018").asInt())
        .findAny()
        .get();
  }

  /**
   * Load State population data from included JSON file.
   */
  ArrayNode loadStatePopulationData() throws IOException {

    ObjectMapper mapper = new ObjectMapper();
    InputStream iStream = getClass().getResourceAsStream("/states.json");
    JsonNode root = mapper.readTree(iStream);

    return (ArrayNode) root.get("data");
  }

  /**
   * Load CSV files for each day into a Map of file names to
   * StateDayEntry for that State and that day's data contained
   * in the file.
   */
  public List<StateDayEntry> loadData() throws IOException {

    System.out.println("Loading...");

    List<StateDayEntry> entries = new ArrayList<>(50);

//    List<Path> sorted = Files.list(Path.of(repoDir))
//        .filter(file -> file.getFileName().toString().endsWith(".csv"))
//        .sorted()
//        .collect(Collectors.toList());
//
//    // Only load last 'numDays' days of data.
////    sorted = sorted.subList(sorted.size() - numDays, sorted.size());
//   // System.out.println("sorted = " + sorted);
//
//    sorted.forEach(file -> {
//
//      try {
//
//        FileReader reader = new FileReader(file.toFile());
//        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
//        List<CSVRecord> records = parser.getRecords();
//
//        for (String state : States.SYMBOLS.keySet()) {
//
//          StateDayEntry entry = new StateDayEntry();
//          entry.setFileName(file.getFileName().toString());
//          entry.setName(state);
//          entry.setSymbol(States.SYMBOLS.get(state));
//          entries.add(entry);
//
//          Optional<Integer> cumulativeCount = records.parallelStream()
//              .filter(r -> r.size() > 3)
//              .filter(isState(state))
//              .map(getCumulativeCount)
//              .reduce(Integer::sum);
//
//          cumulativeCount.ifPresent(entry::setCumulative);
//
//          Optional<Integer> activeCount = records.parallelStream()
//              .filter(r -> r.size() > 3)
//              .filter(isState(state))
//              .map(getActiveCount)
//              .reduce(Integer::sum);
//
//          activeCount.ifPresent(entry::setActive);
//
//          Optional<Integer> deathsCount = records.parallelStream()
//              .filter(r -> r.size() > 3)
//              .filter(isState(state))
//              .map(getDeathsCount)
//              .reduce(Integer::sum);
//
//          deathsCount.ifPresent(entry::setDeaths);
//        }
//
//      } catch (IOException e) {
//        LOG.error(e.toString(), e);
//      }
//
//    });

    return entries;
  }

}
