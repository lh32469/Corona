package org.gpc4j.corona.beans;

import org.gpc4j.corona.States;
import org.primefaces.model.chart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
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

  /**
   * Value of QueryParam for explicitly exluding States to display.
   */
  private String exclude;

  @PostConstruct
  public void postConstruct() {

    Map<String, String> params = FacesContext.getCurrentInstance().
        getExternalContext().getRequestParameterMap();

    states = params.get("states");
    if (states != null) {
      states = states.toUpperCase();
    }

    exclude = params.get("exclude");
    if (exclude != null) {
      exclude = exclude.toUpperCase();
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

    if (exclude != null) {
      chart.setTitle(chart.getTitle() + " (Highest " + maxStates + ", excluding: " + exclude + ")");
      chartSeries
          .filter(series -> {
            // Split off just the State name, not the value.
            String symbol = States.SYMBOLS.get(series
                .getLabel()
                .split("\\(")[0]
                .trim());
            return symbol != null && !exclude.contains(symbol);
          })
          .limit(maxStates)
          .forEachOrdered(chart::addSeries);
    } else if (states != null) {
      chartSeries
          .filter(series -> {
            // Split off just the State name, not the value.
            String symbol = States.SYMBOLS.get(series
                .getLabel()
                .split("\\(")[0]
                .trim());
            return symbol != null && states.contains(symbol);
          })
          .forEachOrdered(chart::addSeries);
    } else {
      // By default, only show top 'maxStates' number of States.
      chart.setTitle(chart.getTitle() + " (Highest " + maxStates + ")");
      chartSeries
          .limit(maxStates)
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
