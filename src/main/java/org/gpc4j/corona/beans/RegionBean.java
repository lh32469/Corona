package org.gpc4j.corona.beans;

import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.OrderingType;
import org.apache.logging.log4j.util.Strings;
import org.gpc4j.corona.States;
import org.gpc4j.corona.dto.StateDayEntry;
import org.gpc4j.corona.raven.StateEntry;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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

  public static final DateTimeFormatter DTF =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
  @Inject
  RavenBean ravenBean;

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
  private String include;

  /**
   * Value for explicitly exluding States to display.
   */
  private String exclude;

  @PostConstruct
  public void postConstruct() {

    include = sBean.getInclude();
    if (include != null) {
      include = include.toUpperCase();
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
//    processModel(cumulativeGraph);

    return cumulativeGraph;
  }


  public LineChartModel getCumulativePC() {

    if (cumulativeGraphPerCapita != null) {
      return cumulativeGraphPerCapita;
    }

    cumulativeGraphPerCapita = getChart(entry -> {
      final int capita = dataBean.getPopulation(entry.getSymbol()) / 100000;
      return entry.getCumulative() / capita;
    });
    cumulativeGraphPerCapita.setTitle("Active Cases by State, Per Capita");
    processModel(cumulativeGraphPerCapita);

    return cumulativeGraphPerCapita;
  }


//  public LineChartModel getActive() {
//    if (activeGraph != null) {
//      return activeGraph;
//    }
//
//    activeGraph = getChart(e -> e.getActive());
//    activeGraph.setTitle("Active Cases by State");
//    processModel(activeGraph);
//
//    return activeGraph;
//  }
//
//  public LineChartModel getActivePC() {
//    if (activeGraphPerCapita != null) {
//      return activeGraphPerCapita;
//    }
//
//    activeGraphPerCapita = getChart(entry -> {
//      final int capita = dataBean.getPopulation(entry.getName()) / 100000;
//      return entry.getActive() / capita;
//    });
//    activeGraphPerCapita.setTitle("Active Cases by State, Per Capita");
//    processModel(activeGraphPerCapita);
//
//    return activeGraphPerCapita;
//  }


  public LineChartModel getDeaths() {
    if (deathsGraph != null) {
      return deathsGraph;
    }

    deathsGraph = getChart("deaths", e -> e.getDeaths());
    deathsGraph.setTitle("Deaths by State");
//    processModel(deathsGraph);

    return deathsGraph;
  }


  public LineChartModel getDeathsPC() {
    if (deathsGraphPerCapita != null) {
      return deathsGraphPerCapita;
    }

    deathsGraphPerCapita = getChart(entry -> {
      final int capita = dataBean.getPopulation(entry.getSymbol()) / 100000;
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

    LineChartModel chart = getRates(entry ->  entry.getCumulative());
//      final float capita = dataBean.getPopulation(entry.getState()) / 100000.0f;
//      return entry.getCumulative() / capita;
//    });

    chart.setTitle("Daily New Cases Per Capita by State");
    processModel(chart);
    return chart;

  }

//  public LineChartModel getNewCaseRates3DayMA() {
//
//    LineChartModel chart = getRates(entry -> {
//      final float capita = dataBean.getPopulation(entry.getName()) / 100000.0f;
//      return entry.getCumulative() / capita;
//    });
//
//    chart.setTitle("Daily New Cases Per Capita by State");
//    processModel(chart);
//    return chart;
//  }

  public LineChartModel getRates(final Function<StateEntry, Integer> func) {
    LineChartModel chart = createChart();

    LocalDate now = LocalDate.now();

    try (IDocumentSession session = ravenBean.getSession()) {

      List<StateEntry> todaysData = session.query(StateEntry.class)
          .whereEquals("day", DTF.format(now.minusDays(1)))
          .toList();

      List<StateEntry> yesterdaysData = session.query(StateEntry.class)
          .whereEquals("day", DTF.format(now.minusDays(2)))
          .toList();

      // Map of highest rate to State symbol
      Map<Float, String> rates = new TreeMap<>();

      for (StateEntry entry : todaysData) {
        final String stateSymbol = entry.getSymbol();
        final int todaysCount = func.apply(entry);
        Optional<StateEntry> previous = yesterdaysData.stream()
            .filter(e -> e.getSymbol().equals(stateSymbol))
            .findFirst();

        if (previous.isPresent()) {
          final int yesterdaysCount = func.apply(previous.get());
          float rounded = Math.round((todaysCount - yesterdaysCount) * 10) / 10.0f;
          final float capita = dataBean.getPopulation(entry.getState()) / 100000.0f;
          rates.put(rounded / capita, stateSymbol);
        }

      }

      LOG.info("rates = " + rates);
    }


//    // Collect data for each State and populate charts.
//    for (String stateName : States.NAME_TO_SYMBOL.keySet()) {
//      LineChartSeries series = new LineChartSeries(stateName);
//      chart.addSeries(series);
//
//      float yesterdaysCount = 0;
//
//      for (String dailyReport : dataBean.getSortedDays()) {
//
//        final String[] array = dailyReport.split("-");
//        final String xValue = array[0] + "/" + array[1];
//
//        // Get count for the State on the day
//        StateDayEntry today = dataBean.getEntry(dailyReport, stateName);
//        float todaysCount = func.apply(today);
//
//        if (yesterdaysCount == 0) {
//          // To avoid big spike at start of graph
//          yesterdaysCount = todaysCount;
//        }
//
//        // Round to one decimal place
//        float rounded = Math.round((todaysCount - yesterdaysCount) * 10) / 10.0f;
//        series.set(xValue, rounded);
//        yesterdaysCount = todaysCount;
//      }
//    }

    return chart;
  }


  public LineChartModel getRates3DayMA(final Function<StateDayEntry, Float> func) {
    LineChartModel chart = createChart();

    // Collect data for each State and populate charts.
    for (String stateName : States.NAME_TO_SYMBOL.keySet()) {
      LineChartSeries series = new LineChartSeries(stateName);
      chart.addSeries(series);

      String[] days = dataBean.getSortedDays().toArray(new String[0]);

      for (int i = 3; i < days.length; i++) {
        StateDayEntry start = dataBean.getEntry(days[i - 3], stateName);

        final String[] array = days[i].split("-");
        final String xValue = array[0] + "/" + array[1];

        StateDayEntry finish = dataBean.getEntry(days[i], stateName);

        float rounded = Math.round(
            ((func.apply(finish) - func.apply(start)) * 10) / 30.0f);

        series.set(xValue, rounded);
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
            String symbol = States.NAME_TO_SYMBOL.get(series
                .getLabel()
                .split("\\(")[0]
                .trim());
            return symbol != null && !exclude.contains(symbol);
          })
          .limit(maxStates)
          .peek(updateLabel())
          .forEachOrdered(chart::addSeries);
    } else if (Strings.isNotEmpty(include)) {
      oldSeriesList.stream()
          .filter(series -> {
            // Split off just the State name, not the value.
            String symbol = States.NAME_TO_SYMBOL.get(series
                .getLabel()
                .split("\\(")[0]
                .trim());
            return symbol != null && include.contains(symbol);
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
  public LineChartModel getChartOld(final Function<StateDayEntry, Integer> func) {

    final LineChartModel chart = createChart();

    // Collect data for each State and populate charts.
    for (String stateName : States.NAME_TO_SYMBOL.keySet()) {
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

  public LineChartModel getChart(final Function<StateEntry, Integer> func) {
    return getChart("cumulative", func);
  }

  List<String> getDaysToDisplay() {
    /*
     * Decide which days to display.
     */
    LocalDate now = LocalDate.now();
    List<String> days = new LinkedList<>();
    for (int i = 0; i < 30; i++) {
      LocalDate day = now.minusDays(i);
      days.add(DTF.format(day));
    }
    LOG.debug("days = " + days);
    return days;
  }

  /**
   * Decide which State data to display based on user input, if any.
   *
   * @param ordering What to order States by. Ex cumulative,rates,etc.
   * @return
   */
  List<String> getStatesToDisplay(final String ordering) {

    LOG.debug("ordering = " + ordering);

    List<String> statesToDisplay;
    LocalDate now = LocalDate.now();
    LOG.debug("today = " + DTF.format(now));

    if (Strings.isNotBlank(include)) {
      // Only get specified
      statesToDisplay = Arrays.asList(include.split(","));
      LOG.debug("include = " + include);
    } else {
      // Get Today's top 10
      try (IDocumentSession session = ravenBean.getSession()) {
        statesToDisplay = session.query(StateEntry.class)
            .whereEquals("day", DTF.format(now.minusDays(1)))
            .orderByDescending(ordering, OrderingType.LONG)
            .take(10)
            .toList()
            .stream()
            .map(StateEntry::getSymbol)
            .collect(Collectors.toList());
      }

      LOG.debug("Top10 = " + statesToDisplay);
    }

    LOG.debug("States to display: " + statesToDisplay);
    return statesToDisplay;
  }

  public LineChartModel getChart(final String ordering,
                                 final Function<StateEntry, Integer> func) {

    final LineChartModel chart = createChart();

    List<String> statesToDisplay = getStatesToDisplay(ordering);
    LOG.debug("States to display: " + statesToDisplay);

    try (IDocumentSession session = ravenBean.getSession()) {

      /*
       * Get selected data from database
       */
      List<StateEntry> entries = session.query(StateEntry.class)
          .whereIn("day", new LinkedList<>(getDaysToDisplay()))
          .whereIn("symbol", new LinkedList<>(statesToDisplay))
          .orderBy("day")
          .toList();

      LOG.debug("Number of StateEntries found = " + entries.size());

      /*
       * Create chart entries for each State's data to display.
       */
      for (Object stateSymbol : statesToDisplay) {
        // Get full name of State for label
        final String label = States.SYMBOL_TO_NAME.get(stateSymbol.toString());
        LineChartSeries series = new LineChartSeries(label);
        chart.addSeries(series);

        // Find all the days for this State.
        entries.stream()
            .filter(e -> e.getSymbol().equals(stateSymbol))
            .forEach(entry -> {
              final String[] array = entry.getDay().split("-");
              String month = array[1];
              String day = array[2];
              if (month.startsWith("0")) {
                month = month.substring(1);
              }
              if (day.startsWith("0")) {
                day = day.substring(1);
              }
              final String xValue = month + "/" + day;
              series.set(xValue, func.apply(entry));
            });

      }

    }

    return chart;
  }

  /**
   * Calculate average number of new cases for the List of
   * StateDayEntries provided.
   */
  Function<List<StateDayEntry>, Float> averageNewCases = list -> {

    StateDayEntry first = list.get(0);
    StateDayEntry last = list.get(list.size() - 1);

    return (float) ((last.getCumulative() - first.getCumulative()) / list.size());
  };

}
