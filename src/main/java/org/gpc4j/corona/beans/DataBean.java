package org.gpc4j.corona.beans;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.gpc4j.corona.States;
import org.gpc4j.corona.dto.StateDayEntry;
import org.primefaces.model.chart.LineChartSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
public class DataBean implements Serializable {

  private List<LineChartSeries> cumulative;
  private List<LineChartSeries> active;
  private List<LineChartSeries> recovered;
  private List<LineChartSeries> deaths;

  @Value("${corona.data.repo}")
  String repoDir;

  static Logger LOG = LoggerFactory.getLogger(DataBean.class);

  /**
   * Update LineChartSeries to include last value of
   * the series in the Label.
   */
  private static Consumer<LineChartSeries> updateLabel() {
    return series -> {
      Object[] array = series.getData().values().toArray();
      if (array.length > 0) {
        Object last = array[array.length - 1];
        series.setLabel(series.getLabel() + " (" + last + ")");
      }
    };
  }

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


  @PostConstruct
  public void postConstruct() {
    LOG.info("postConstruct");

    cumulative = new LinkedList<>();
    active = new LinkedList<>();
    deaths = new LinkedList<>();

    List<StateDayEntry> entries = null;
    try {
      entries = loadData();
    } catch (IOException e) {
      LOG.error(e.toString(), e);
      return;
    }

    // Get days in list
    Set<String> days = entries.parallelStream()
        .map(entry -> entry.getFileName())
        .collect(Collectors.toSet());

    // Sort the results
    days = new TreeSet<>(days);

    // Collect data for each State and populate charts.
    for (String stateName : States.SYMBOLS.keySet()) {

      LineChartSeries stateCumulativeSeries = new LineChartSeries(stateName);
      LineChartSeries stateActiveSeries = new LineChartSeries(stateName);
      LineChartSeries stateDeathsSeries = new LineChartSeries(stateName);

      cumulative.add(stateCumulativeSeries);
      active.add(stateActiveSeries);
      deaths.add(stateDeathsSeries);

      for (String dailyReport : days) {
        final String[] array = dailyReport.split("-");
        final String xValue = array[0] + "/" + array[1];

        // Get count for the State on the day
        StateDayEntry entry = entries.parallelStream()
            .filter(e -> e.getFileName().equals(dailyReport))
            .filter(e -> e.getName().equals(stateName))
            .findAny().get();

        stateCumulativeSeries.set(xValue, entry.getCumulative());
        stateActiveSeries.set(xValue, entry.getActive());
        stateDeathsSeries.set(xValue, entry.getDeaths());
      }

    }

    cumulative.forEach(updateLabel());
    active.forEach(updateLabel());
    deaths.forEach(updateLabel());

    cumulative = sortCharts(cumulative);
    active = sortCharts(active);
    deaths = sortCharts(deaths);

    LOG.info("Cumulative LineChartSeries: " + cumulative.size());
    LOG.info("Active LineChartSeries: " + active.size());
    LOG.info("Deaths LineChartSeries: " + deaths.size());
  }

  public Stream<LineChartSeries> getCumulative() {
    return cumulative.parallelStream();
  }

  public Stream<LineChartSeries> getActive() {
    return active.parallelStream();
  }

  public Stream<LineChartSeries> getRecovered() {
    return recovered.parallelStream();
  }

  public Stream<LineChartSeries> getDeaths() {
    return deaths.parallelStream();
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

    sorted = sorted.subList(sorted.size() - 14, sorted.size());

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
              .reduce((a, b) -> a + b);

          cumulativeCount.ifPresent(entry::setCumulative);

          Optional<Integer> activeCount = records.parallelStream()
              .filter(r -> r.size() > 3)
              .filter(isState(state))
              .map(getActiveCount)
              .reduce((a, b) -> a + b);

          activeCount.ifPresent(entry::setActive);

          Optional<Integer> deathsCount = records.parallelStream()
              .filter(r -> r.size() > 3)
              .filter(isState(state))
              .map(getDeathsCount)
              .reduce((a, b) -> a + b);

          deathsCount.ifPresent(entry::setDeaths);
        }

      } catch (IOException e) {
        LOG.error(e.toString(), e);
      }

    });

    return entries;
  }

  List<LineChartSeries> sortCharts(Collection<LineChartSeries> chartSeries) {

    List<LineChartSeries> sorted = chartSeries.stream()
        .sorted((s1, s2) -> {

          Object[] array1 = s1.getData().values().toArray();
          Object[] array2 = s2.getData().values().toArray();

          if (array1.length == 0 && array2.length > 0) {
            return -1;
          }
          if (array2.length == 0 && array1.length > 0) {
            return +1;
          }
          if (array2.length == 0 && array1.length == 0) {
            return 0;
          }

          int s1M = Integer.parseInt(array1[array1.length - 1].toString());
          int s2M = Integer.parseInt(array2[array2.length - 1].toString());

          return s2M - s1M;
        })
        .collect(Collectors.toList());

    return sorted;
  }


}
