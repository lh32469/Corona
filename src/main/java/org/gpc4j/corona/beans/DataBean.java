package org.gpc4j.corona.beans;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
public class DataBean implements Serializable {

  private List<LineChartSeries> cumulative;
  private List<LineChartSeries> active;

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
      Object last = array[array.length - 1];
      series.setLabel(series.getLabel() + " (" + last + ")");
    };
  }

  /**
   * Get the total number of cumulative cases from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getCumulative =
      record -> Integer.parseInt(record.get(3));

  /**
   * Get the number of currently active cases from the CSVRecord.
   */
  static final Function<CSVRecord, Integer> getActive =
      record -> {
        int confirmed = 0;
        int deaths = 0;
        int recovered = 0;

        try {
          confirmed = Integer.parseInt(record.get(3));
        } catch (NumberFormatException ex) {
          LOG.warn(ex + "\n" + record);
        }
        try {
          deaths = Integer.parseInt(record.get(4));
        } catch (NumberFormatException ex) {
          LOG.warn(ex + "\n" + record);
        }
        try {
          recovered = Integer.parseInt(record.get(5));
        } catch (NumberFormatException ex) {
          LOG.warn(ex + "\n" + record);
        }

        return confirmed - deaths - recovered;
      };


  @PostConstruct
  public void postConstruct() {
    LOG.info("postConstruct");
    try {

      final Map<String, List<CSVRecord>> data = loadData();

      active = processData(data, getActive);
      active.forEach(updateLabel());

      cumulative = processData(data, getCumulative);
      cumulative.forEach(updateLabel());

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Stream<LineChartSeries> getCumulative() {
    return cumulative.parallelStream();
  }

  public Stream<LineChartSeries> getActive() {
    return active.parallelStream();
  }

  public Map<String, List<CSVRecord>> loadData() throws IOException {

    LOG.info("Loading data from: " + repoDir);

    Stream<Path> files = Files.list(Path.of(repoDir));
    Map<String, List<CSVRecord>> data = new TreeMap<>();

    files
        .filter(f -> f.toString().endsWith(".csv"))
        .forEach(f -> {
          try {

            FileReader reader = new FileReader(f.toFile());
            CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
            List<CSVRecord> records = parser.getRecords().parallelStream()
                .filter(r -> r.size() > 3)
                .filter(r -> "US".equals(r.get(1)))
                .filter(r -> r.get(2).startsWith("2020"))
                .filter(r -> !r.get(0).contains(","))
                .collect(Collectors.toList());

            if (!records.isEmpty()) {
              data.put(f.getFileName().toString(), records);
            }

          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    LOG.info("Loaded " + data.size() + " files");

    return data;
  }


  List<LineChartSeries> processData(
      Map<String, List<CSVRecord>> data,
      Function<CSVRecord, Integer> function) {

    Map<String, LineChartSeries> chartSeriesMap = new HashMap<>();

    int index = 0;
    for (String day : data.keySet()) {
      List<CSVRecord> dayData = data.get(day);
      for (CSVRecord stateRecord : dayData) {

        final String stateName = stateRecord.get(0);
        LineChartSeries stateSeries = chartSeriesMap.get(stateName);
        if (stateSeries == null) {
          stateSeries = new LineChartSeries();
          stateSeries.setLabel(stateName);
          chartSeriesMap.put(stateName, stateSeries);
        }

        stateSeries.set(index, function.apply(stateRecord));
      }

      index++;
    }

    LOG.info("Added " + chartSeriesMap.values().size() + " states.");

    return sortCharts(chartSeriesMap.values());
  }

  List<LineChartSeries> sortCharts(Collection<LineChartSeries> chartSeries) {

    List<LineChartSeries> sorted = chartSeries.stream()
        .sorted((s1, s2) -> {

          int s1Max = s1.getData()
              .values()
              .stream()
              .mapToInt(i -> i.intValue())
              .max()
              .getAsInt();

          int s2Max = s2.getData()
              .values()
              .stream()
              .mapToInt(i -> i.intValue())
              .max()
              .getAsInt();

          return s2Max - s1Max;
        })
        .collect(Collectors.toList());

    return sorted;
  }

}
