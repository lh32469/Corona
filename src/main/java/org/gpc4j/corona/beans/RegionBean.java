package org.gpc4j.corona.beans;

import org.apache.logging.log4j.util.Strings;
import org.gpc4j.corona.States;
import org.gpc4j.corona.dto.StateDayEntry;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author Lyle T Harris
 */
@Named("region")
@Scope("request")
public class RegionBean {

  LineChartModel activeGraph;
  LineChartModel deathsGraph;
  LineChartModel recoveredGraph;
  LineChartModel cumulativeGraph;
  LineChartModel cumulativeGraphPerCapita;
  LineChartModel activeGraphPerCapita;
  LineChartModel deathsGraphPerCapita;

  /**
   * Get the active count from the StateDayEntry.
   */
  static final Function<StateDayEntry, Integer> getActive =
      entry -> entry.getActive();

  /**
   * Update LineChartSeries to include last value of
   * the series in the Label.
   */
  private static Consumer<? super ChartSeries> updateLabel() {
    return series -> {
      Object[] array = series.getData().values().toArray();
      if (array.length > 0) {
        Object last = array[array.length - 1];
        series.setLabel(series.getLabel() + " (" + last + ")");
      }
    };
  }

  final static private Logger LOG
      = LoggerFactory.getLogger(RegionBean.class);

  /**
   * Application Scoped Bean containing data.
   */
  @Inject
  private DataBean dataBean;

  /**
   * Session Bean containing user input.
   */
  @Inject
  private SessionBean sBean;

  /**
   * Default number of States to display if no QueryParam
   */
  @Value("${corona.states}")
  int maxStates;

  /**
   * Value for explicitly selecting States to display.
   */
  private String states;

  /**
   * Value for explicitly exluding States to display.
   */
  private String exclude;

  @PostConstruct
  public void postConstruct() {

    states = sBean.getInclude();
    if (states != null) {
      states = states.toUpperCase();
    }

    exclude = sBean.getExclude();
    if (exclude != null) {
      exclude = exclude.toUpperCase();
    }

  }

  public LineChartModel getCumulative() {

    if (cumulativeGraph != null) {
      return cumulativeGraph;
    }

    cumulativeGraph = getChart(entry -> entry.getCumulative());
    cumulativeGraph.setTitle("Cumulative Cases by State");
    processModel(cumulativeGraph);

    return cumulativeGraph;
  }


  public LineChartModel getCumulativePC() {

    if (cumulativeGraphPerCapita != null) {
      return cumulativeGraphPerCapita;
    }

    cumulativeGraphPerCapita = getChart(entry -> {
      final int capita = dataBean.getPopulation(entry.getName()) / 100000;
      return entry.getCumulative() / capita;
    });
    cumulativeGraphPerCapita.setTitle("Active Cases by State, Per Capita");
    processModel(cumulativeGraphPerCapita);

    return cumulativeGraphPerCapita;
  }


  public LineChartModel getActive() {
    if (activeGraph != null) {
      return activeGraph;
    }

    activeGraph = getChart(e -> e.getActive());
    activeGraph.setTitle("Active Cases by State");
    processModel(activeGraph);

    return activeGraph;
  }

  public LineChartModel getActivePC() {
    if (activeGraphPerCapita != null) {
      return activeGraphPerCapita;
    }

    activeGraphPerCapita = getChart(entry -> {
      final int capita = dataBean.getPopulation(entry.getName()) / 100000;
      return entry.getActive() / capita;
    });
    activeGraphPerCapita.setTitle("Active Cases by State, Per Capita");
    processModel(activeGraphPerCapita);

    return activeGraphPerCapita;
  }


  public LineChartModel getDeaths() {
    if (deathsGraph != null) {
      return deathsGraph;
    }

    deathsGraph = getChart(e -> e.getDeaths());
    deathsGraph.setTitle("Deaths by State");
    processModel(deathsGraph);

    return deathsGraph;
  }


  public LineChartModel getDeathsPC() {
    if (deathsGraphPerCapita != null) {
      return deathsGraphPerCapita;
    }

    deathsGraphPerCapita = getChart(entry -> {
      final int capita = dataBean.getPopulation(entry.getName()) / 100000;
      return entry.getDeaths() / capita;
    });
    deathsGraphPerCapita.setTitle("Deaths by State, Per Capita");
    processModel(deathsGraphPerCapita);

    return deathsGraphPerCapita;
  }


  public LineChartModel getRecovered() {
    if (recoveredGraph != null) {
      return recoveredGraph;
    }

    recoveredGraph = getChart(e -> e.getCumulative() - e.getDeaths());
    recoveredGraph.setTitle("Recovered Cases by State");
    processModel(recoveredGraph);

    return recoveredGraph;
  }

  public LineChartModel getNewCaseRates() {

    LineChartModel chart = getRates(entry -> {
      final float capita = dataBean.getPopulation(entry.getName()) / 100000.0f;
      return entry.getCumulative() / capita;
    });

    chart.setTitle("Daily New Cases Per Capita by State");
    processModel(chart);
    return chart;
  }

  public LineChartModel getRates(final Function<StateDayEntry, Float> func) {
    LineChartModel chart = createChart();

    // Collect data for each State and populate charts.
    for (String stateName : States.SYMBOLS.keySet()) {
      LineChartSeries series = new LineChartSeries(stateName);
      chart.addSeries(series);

      float yesterdaysCount = 0;

      for (String dailyReport : dataBean.getSortedDays()) {

        final String[] array = dailyReport.split("-");
        final String xValue = array[0] + "/" + array[1];

        // Get count for the State on the day
        StateDayEntry today = dataBean.getEntry(dailyReport, stateName);
        float todaysCount = func.apply(today);

        if (yesterdaysCount == 0) {
          // To avoid big spike at start of graph
          yesterdaysCount = todaysCount;
        }

        // Round to one decimal place
        float rounded = Math.round((todaysCount - yesterdaysCount) * 10) / 10.0f;
        series.set(xValue, rounded);
        yesterdaysCount = todaysCount;
      }
    }

    return chart;
  }

  /**
   * Apply rules based on QueryParams to ChartSeries
   * in the LineChartModel provided.
   */
  void processModel(final LineChartModel chart) {

    List<ChartSeries> oldSeriesList = sortCharts(chart.getSeries());
    chart.getSeries().clear();

    if (Strings.isNotBlank(exclude)) {
      chart.setTitle(chart.getTitle() + " (Highest " + maxStates + ", excluding: " + exclude + ")");
      oldSeriesList.stream()
          .filter(series -> {
            // Split off just the State name, not the value.
            String symbol = States.SYMBOLS.get(series
                .getLabel()
                .split("\\(")[0]
                .trim());
            return symbol != null && !exclude.contains(symbol);
          })
          .limit(maxStates)
          .peek(updateLabel())
          .forEachOrdered(chart::addSeries);
    } else if (Strings.isNotEmpty(states)) {
      oldSeriesList.stream()
          .filter(series -> {
            // Split off just the State name, not the value.
            String symbol = States.SYMBOLS.get(series
                .getLabel()
                .split("\\(")[0]
                .trim());
            return symbol != null && states.contains(symbol);
          })
          .peek(updateLabel())
          .forEachOrdered(chart::addSeries);
    } else {
      // By default, only show top 'maxStates' number of States.
      chart.setTitle(chart.getTitle() + " (Highest " + maxStates + ")");
      oldSeriesList.stream()
          .limit(maxStates)
          .peek(updateLabel())
          .forEachOrdered(chart::addSeries);
    }
  }


  LineChartModel createChart() {

    LineChartModel chart = new LineChartModel();
    chart.setLegendPosition("n");
    chart.setShowPointLabels(true);
    chart.setShowDatatip(true);

    chart.getAxes().put(AxisType.X, new CategoryAxis("Days"));

    Axis yAxis = chart.getAxis(AxisType.Y);
    yAxis.setLabel("# Cases");
    yAxis.setMin(0);

    return chart;
  }


  List<ChartSeries> sortCharts(Collection<ChartSeries> chartSeries) {

    List<ChartSeries> sorted = chartSeries.stream()
        .sorted((s1, s2) -> {

          Object[] array1 = s1.getData().values().toArray();
          Object[] array2 = s2.getData().values().toArray();

          if (array1.length == 0 && array2.length > 0) {
            return -1;
          }
          if (array2.length == 0 && array1.length > 0) {
            return +1;
          }
          if (array2.length == 0) {
            return 0;
          }

          float s1M = Float.parseFloat(array1[array1.length - 1].toString());
          float s2M = Float.parseFloat(array2[array2.length - 1].toString());

          return (int) (s2M - s1M);
        })
        .collect(Collectors.toList());

    return sorted;
  }


  /**
   * Get the LineChartModel for all States using the Function provided
   * to provide the value for each StateDayEntry.
   */
  public LineChartModel getChart(final Function<StateDayEntry, Integer> func) {

    final LineChartModel chart = createChart();

    // Collect data for each State and populate charts.
    for (String stateName : States.SYMBOLS.keySet()) {
      LineChartSeries series = new LineChartSeries(stateName);
      chart.addSeries(series);

      // Get population for calculating per 100,000 people.
      final int capita = dataBean.getPopulation(stateName) / 100000;

      for (String dailyReport : dataBean.getSortedDays()) {
        final String[] array = dailyReport.split("-");
        final String xValue = array[0] + "/" + array[1];

        // Get count for the State on the day
        StateDayEntry entry = dataBean.getEntry(dailyReport, stateName);
        series.set(xValue, func.apply(entry));
      }
    }

    return chart;
  }


}
