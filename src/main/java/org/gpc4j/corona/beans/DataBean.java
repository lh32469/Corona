package org.gpc4j.corona.beans;

import org.primefaces.model.chart.LineChartSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
public class DataBean implements Serializable {

  private List<LineChartSeries> lineChartSeries;

  @Value("${corona.data.repo}")
  String repoDir;

  final static private Logger LOG
      = LoggerFactory.getLogger(DataBean.class);


  @PostConstruct
  public void postConstruct() {
    LOG.info("postConstruct");
    try {
      lineChartSeries = processData(loadData());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Stream<LineChartSeries> getLineChartSeries() {
    return lineChartSeries.parallelStream();
  }

  public Map<String, List<String>> loadData() throws IOException {

    LOG.info("Loading data from: " + repoDir);

    Map<String, List<String>> data = new TreeMap<>();

    Stream<Path> files = Files.list(Path.of(repoDir));

    files.forEach(f -> {
      try {
        List<String> lines = Files.readAllLines(f)
            .stream()
            .skip(1)
            .filter(line -> !line.contains("Princess"))
//            .peek(line -> LOG.info("line: " + line))
            .filter(line -> {
              String[] array = line.split(",");
              return array[1].equals("US");
            })
            .collect(Collectors.toList());
        if (!lines.isEmpty()) {
          data.put(f.getFileName().toString(), lines);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    LOG.info("Loaded: " + data.size() + " entries");

    return data;
  }


  public List<LineChartSeries> processData(Map<String, List<String>> data) {

    Map<String, LineChartSeries> chartSeriesMap = new HashMap<>();

    int index = 0;

    for (String day : data.keySet()) {
      //LOG.info("Adding Day: " + day);
      List<String> dayData = data.get(day);
      for (String stateData : dayData) {
        String[] array = stateData.split(",");
//        System.out.println("state: " + array[0] +
//            " = " + array[3]);

        LineChartSeries state = chartSeriesMap.get(array[0]);
        if (state == null) {
          state = new LineChartSeries();
          state.setLabel(array[0]);
          chartSeriesMap.put(array[0], state);
        }

        state.set(index, Integer.parseInt(array[3]));
      }

      index++;
    }

    LOG.info("Added " + chartSeriesMap.values().size() + " states.");
    return chartSeriesMap.values().stream()
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
  }


  public static void main(String[] args) throws IOException {
    DataBean bean = new DataBean();
    Map<String, List<String>> data = bean.loadData();
    System.out.println("data = " + data);

    Collection<LineChartSeries> processed = bean.processData(data);
    System.out.println("chartSeriesMap = " + bean.processData(data));

    LineChartSeries oregon = processed.stream()
        .filter(s -> s.getLabel().equals("Oregon"))
        .findAny()
        .get();

    System.out.println("oregon.getData() = " + oregon.getData());
  }
}
