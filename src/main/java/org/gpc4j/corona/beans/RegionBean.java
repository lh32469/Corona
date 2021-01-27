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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  DataBean dataBean;
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

    LineChartModel chart = getRates(entry -> entry.getCumulative());
//      final float capita = dataBean.getPopulation(entry.getState()) / 100000.0f;
//      return entry.getCumulative() / capita;
//    });

    chart.setTitle("Daily New Cases Per Capita by State");
//    processModel(chart);
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
    List<String> statesToDisplay;


    try (IDocumentSession session = ravenBean.getSession()) {


      if (Strings.isNotBlank(include)) {
        // Only get specified
        statesToDisplay = Arrays.asList(include.split(","));
        LOG.debug("include = " + include);
      } else {

        // Needs to be List of Objects for .whereIn clause
        List<Object> days = Arrays.asList(
            DTF.format(now.minusDays(0)),
            DTF.format(now.minusDays(1)));

        List<StateEntry> recentData = session.query(StateEntry.class)
            .whereIn("day", days)
            .whereNotEquals("symbol", null)
            .toList();

        // Rates for the last two days.
        Map<String, Float> latestRates = getTheRates(now, States.ALL_SYMBOLS, recentData);
        statesToDisplay = latestRates.entrySet().stream()
            .sorted((e1, e2) -> Math.round(e2.getValue() - e1.getValue()))
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableList());

        LOG.info("Top 10 = " + statesToDisplay);

      }

      /*
       * Load all relevant data.
       */
      List<StateEntry> theData = session.query(StateEntry.class)
          .whereIn("day", new LinkedList<>(getDaysToDisplay()))
          .whereIn("symbol", new LinkedList<>(statesToDisplay))
          .toList();


      for (String stateSymbol : statesToDisplay) {
        final String label = States.SYMBOL_TO_NAME.get(stateSymbol);
        LineChartSeries series = new LineChartSeries(label);
        chart.addSeries(series);

        for (int i = 20; i >= 0; i--) {
          LocalDate day = now.minusDays(i);
          Map<String, Float> rates1 = getTheRates(day, 3, Arrays.asList(stateSymbol), theData);
          series.set(dayToLabel(day), rates1.get(stateSymbol));
          if (i == 0) {
            series.setLabel(label + " (" + rates1.get(stateSymbol) + ")");
          }
        }

      }

    }

    return chart;
  }


  /**
   * Get a Map of rates for the List of State symbols provided on the date
   * provided.  Uses current date's count versus previous date's count to
   * compute the rate.
   *
   * @param day          Date to get rates for
   * @param stateSymbols Symbols of the States to get rates for.
   * @param theData      Relevant data.
   * @return Map of State symbols to rates for the date provided.
   */
  Map<String, Float> getTheRates(final LocalDate day,
                                 final List<String> stateSymbols,
                                 final List<StateEntry> theData) {

    return getTheRates(day, 1, stateSymbols, theData);
  }


  /**
   * Get a Map of moving day average rates for the List of State symbols
   * provided on the date provided.  Uses current date's count versus previous
   * daysToAverage number of date's count to compute the rate.
   *
   * @param day           Date to get rates for
   * @param daysToAverage Number of days to to rolling average
   * @param stateSymbols  Symbols of the States to get rates for.
   * @param theData       Relevant data.
   * @return Map of State symbols to rates for the date provided.
   */
  Map<String, Float> getTheRates(final LocalDate day,
                                 final int daysToAverage,
                                 final List<String> stateSymbols,
                                 final List<StateEntry> theData) {

    Map<String, Float> rates = new HashMap<>();

    for (String stateSymbol : stateSymbols) {

      Optional<StateEntry> current = theData.stream()
          .filter(e -> e.getSymbol().equals(stateSymbol))
          .filter(e -> e.getDay().equals(DTF.format(day)))
          .findAny();

      if (current.isEmpty()) {
        LOG.info(DTF.format(day) + ",  Not Found: " + stateSymbol);
        continue;
      }

      Optional<StateEntry> previous = theData.stream()
          .filter(e -> e.getSymbol().equals(stateSymbol))
          .filter(e -> e.getDay().equals(DTF.format(day.minusDays(daysToAverage))))
          .findAny();

      if (previous.isEmpty()) {
        LOG.info(DTF.format(day.minusDays(daysToAverage)) + ",  Not Found: " + stateSymbol);
        continue;
      }

      final int prev = previous.get().getCumulative();
      final int curr = current.get().getCumulative();
      final float capita = dataBean.getPopulation(current.get().getState()) / 100000.0f;
      int dayAverage = (curr - prev) / daysToAverage;
      float rounded = Math.round((dayAverage * 10) / capita) / 10.0f;
      rates.put(stateSymbol, rounded);
    }

    LOG.trace(day + ":  " + rates);
    return rates;
  }


  /**
   * Apply rules based on QueryParams to ChartSeries
   * in the LineChartModel provided.
   */
  private void processModel(final LineChartModel chart) {

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
   * Convert date provied to label suitable for x-axis labeling.
   * Ex 2021-01-23 becomes 1/23
   */
  String dayToLabel(LocalDate date) {
    final String[] array = DTF.format(date).split("-");
    String month = array[1];
    String day = array[2];
    if (month.startsWith("0")) {
      month = month.substring(1);
    }
    if (day.startsWith("0")) {
      day = day.substring(1);
    }
    final String xValue = month + "/" + day;
    return xValue;
  }


}
