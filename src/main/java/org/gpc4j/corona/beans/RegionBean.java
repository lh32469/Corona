package org.gpc4j.corona.beans;

import org.primefaces.model.chart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;


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

  static final Map<String, String> syms = new HashMap<>();

  static {

    syms.put("Alabama", "AL");
    syms.put("Alaska", "AK");
    syms.put("Arizona", "AZ");
    syms.put("Arkansas", "AR");
    syms.put("California", "CA");
    syms.put("Colorado", "CO");
    syms.put("Connecticut", "CT");
    syms.put("Delaware", "DE");
    syms.put("Florida", "FL");
    syms.put("Georgia", "GA");
    syms.put("Hawaii", "HI");
    syms.put("Idaho", "ID");
    syms.put("Illinois", "IL");
    syms.put("Indiana", "IN");
    syms.put("Iowa", "IA");
    syms.put("Kansas", "KS");
    syms.put("Kentucky", "KY");
    syms.put("Louisiana", "LA");
    syms.put("Maine", "ME");
    syms.put("Maryland", "MD");
    syms.put("Massachusetts", "MA");
    syms.put("Michigan", "MI");
    syms.put("Minnesota", "MN");
    syms.put("Mississippi", "MS");
    syms.put("Missouri", "MO");
    syms.put("Montana", "MT");
    syms.put("Nebraska", "NE");
    syms.put("Nevada", "NV");
    syms.put("Ohio", "OH");
    syms.put("Oklahoma", "OK");
    syms.put("Oregon", "OR");
    syms.put("Pennsylvania", "PA");
    syms.put("Tennessee", "TN");
    syms.put("Texas", "TX");
    syms.put("Utah", "UT");
    syms.put("Vermont", "VT");
    syms.put("Virginia", "VA");
    syms.put("Washington", "WA");
    syms.put("Wisconsin", "WI");
    syms.put("Wyoming", "WY");

    syms.put("New Hampshire", "NH");
    syms.put("New Jersey", "NJ");
    syms.put("New Mexico", "NM");
    syms.put("New York", "NY");
    syms.put("North Carolina", "NC");
    syms.put("North Dakota", "ND");
    syms.put("Rhode Island", "RI");
    syms.put("South Carolina", "SC");
    syms.put("South Dakota", "SD");
    syms.put("West Virginia", "WV");
  }

  final static private Logger LOG
      = LoggerFactory.getLogger(RegionBean.class);

  /**
   * Application Scoped Bean containing data.
   */
  @Inject
  private DataBean dataBean;

  /**
   * Default number of States to display if no QueryParam
   */
  @Value("${corona.states}")
  int maxStates;

  /**
   * Value of QueryParam for explicitly selecting States to display.
   */
  private String states;

  @PostConstruct
  public void postConstruct() {

    Map<String, String> params = FacesContext.getCurrentInstance().
        getExternalContext().getRequestParameterMap();

    states = params.get("states");
    if (states != null) {
      states = states.toUpperCase();
    }
  }

  public LineChartModel getCumulative() {
    if (cumulativeGraph != null) {
      return cumulativeGraph;
    }

    cumulativeGraph = createChart("Cumulative Cases by State");
    processGraph(cumulativeGraph, dataBean.getCumulative());

    return cumulativeGraph;
  }

  public LineChartModel getActive() {
    if (activeGraph != null) {
      return activeGraph;
    }

    activeGraph = createChart("Active Cases by State");
    processGraph(activeGraph, dataBean.getActive());

    return activeGraph;
  }

  public LineChartModel getDeaths() {
    if (deathsGraph != null) {
      return deathsGraph;
    }

    deathsGraph = createChart("Deaths by State");
    processGraph(deathsGraph, dataBean.getDeaths());

    return deathsGraph;
  }

  public LineChartModel getRecovered() {
    if (recoveredGraph != null) {
      return recoveredGraph;
    }

    recoveredGraph = createChart("Recovered Cases by State");
    processGraph(recoveredGraph, dataBean.getRecovered());

    return recoveredGraph;
  }

  /**
   * Apply rules based on QueryParams to LineChartSeries
   * provided and populate LineChartModel accordingly.
   */
  void processGraph(
      final LineChartModel chart,
      final Stream<LineChartSeries> chartSeries) {

    if (states == null) {
      // By default, only show top 'maxStates' number of States.
      chart.setTitle(chart.getTitle() + " (Top " + maxStates + ")");
      chartSeries
          .limit(maxStates)
          .forEachOrdered(chart::addSeries);
    } else {
      chartSeries
          .filter(series -> {
            // Split off just the State name, not the value.
            String symbol = syms.get(series
                .getLabel()
                .split("\\(")[0]
                .trim());
            return symbol != null && states.contains(symbol);
          })
          .forEachOrdered(chart::addSeries);
    }
  }


  LineChartModel createChart(final String title) {

    LineChartModel chart = new LineChartModel();
    chart.setTitle(title);
    chart.setLegendPosition("n");
    chart.setShowPointLabels(true);
    chart.setShowDatatip(true);

    chart.getAxes().put(AxisType.X, new CategoryAxis("Days"));

    Axis yAxis = chart.getAxis(AxisType.Y);
    yAxis.setLabel("# Cases");
    yAxis.setMin(0);

    return chart;
  }

}
