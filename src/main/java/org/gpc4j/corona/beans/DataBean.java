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
import java.util.*;
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
    return record -> record.get(0).equals(state) ||
        record.get(2).equals(state);
  }

  /**
   * Get the total number of cumulative cases from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getCumulativeCount =
      record -> {
        if (record.size() == 12) {
          return Integer.parseInt(record.get(7));
        } else {
          try {
            return Integer.parseInt(record.get(3));
          } catch (NumberFormatException ex) {
            return 0;
          }
        }
      };


  /**
   * Get the total number of deaths from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getDeathsCount =
      record -> {
        if (record.size() == 12) {
          return Integer.parseInt(record.get(8));
        } else {
          try {
            return Integer.parseInt(record.get(4));
          } catch (NumberFormatException ex) {
            return 0;
          }
        }
      };

  /**
   * Get the total number of recovered cases from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getRecovered =
      record -> {
        // Ignore entire region entries
        if (record.get(0).equals(record.get(1))) {
          return 0;
        }
        return Integer.parseInt(record.get(5));
      };

  /**
   * Get the number of currently active cases from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getActiveCount =
      record -> {

        int confirmed = 0;
        int deaths = 0;
        int recovered = 0;

        if (record.size() == 12) {
          confirmed = Integer.parseInt(record.get(7));
          deaths = Integer.parseInt(record.get(8));
          recovered = Integer.parseInt(record.get(9));
        } else {
          // Pre 3-23 format
          try {
            confirmed = Integer.parseInt(record.get(3));
            deaths = Integer.parseInt(record.get(4));
            recovered = Integer.parseInt(record.get(5));
          } catch (NumberFormatException ex) {

          }
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

    List<StateDayEntry> entries = new ArrayList<>(50);

    List<Path> sorted = Files.list(Path.of(repoDir))
        .filter(file -> file.getFileName().toString().endsWith(".csv"))
        .sorted()
        .collect(Collectors.toList());

    // Only load last 'numDays' days of data.
    sorted = sorted.subList(sorted.size() - numDays, sorted.size());

    sorted.forEach(file -> {

      try {

        FileReader reader = new FileReader(file.toFile());
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
        List<CSVRecord> records = parser.getRecords();

        for (String state : States.SYMBOLS.keySet()) {

          StateDayEntry entry = new StateDayEntry();
          entry.setFileName(file.getFileName().toString());
          entry.setName(state);
          entry.setSymbol(States.SYMBOLS.get(state));
          entries.add(entry);

          Optional<Integer> cumulativeCount = records.parallelStream()
              .filter(r -> r.size() > 3)
              .filter(isState(state))
              .map(getCumulativeCount)
              .reduce(Integer::sum);

          cumulativeCount.ifPresent(entry::setCumulative);

          Optional<Integer> activeCount = records.parallelStream()
              .filter(r -> r.size() > 3)
              .filter(isState(state))
              .map(getActiveCount)
              .reduce(Integer::sum);

          activeCount.ifPresent(entry::setActive);

          Optional<Integer> deathsCount = records.parallelStream()
              .filter(r -> r.size() > 3)
              .filter(isState(state))
              .map(getDeathsCount)
              .reduce(Integer::sum);

          deathsCount.ifPresent(entry::setDeaths);
        }

      } catch (IOException e) {
        LOG.error(e.toString(), e);
      }

    });

    return entries;
  }

}
