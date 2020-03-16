package org.gpc4j.corona.beans;

import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author Lyle T Harris
 */
@Named("region")
@Scope("request")
public class RegionBean {

  LineChartModel regionGraph;

  static final Map<String, String> syms = new HashMap<>();

  static {
    syms.put("AZ", "Arizona");
    syms.put("CO", "Colorado");
    syms.put("FL", "Florida");
    syms.put("IL", "Illinois");
    syms.put("MI", "Michigan");
    syms.put("NY", "New York");
    syms.put("NJ", "New Jersey");
    syms.put("OR", "Oregon");
    syms.put("CA", "California");
    syms.put("WA", "Washington");
    syms.put("MA", "Massachusetts");
    syms.put("TX", "Texas");
  }

  final static private Logger LOG
      = LoggerFactory.getLogger(RegionBean.class);

  @Inject
  private DataBean dataBean;

  @PostConstruct
  public void postConstruct() {

    LOG.info("Start...");
    LOG.info("DataBean: " + dataBean);

    Map<String, String> params = FacesContext.getCurrentInstance().
        getExternalContext().getRequestParameterMap();
    String states = params.get("states");
    System.out.println("states = " + states);

    regionGraph = new LineChartModel();
    regionGraph.setTitle("Cumulative Cases by State");
    regionGraph.setLegendPosition("n");

    Axis xAxis = regionGraph.getAxis(AxisType.X);
    xAxis.setLabel("# Days");
    xAxis.setMin(0);

    Axis yAxis = regionGraph.getAxis(AxisType.Y);
    yAxis.setLabel("# Cases");
    yAxis.setMin(0);

    Map<String, List<String>> data;

    Collection<LineChartSeries> chartSeriesCollection =
        dataBean.getLineChartSeries();

    if (states == null) {
      chartSeriesCollection = chartSeriesCollection.stream()
          .limit(15)
          .collect(Collectors.toList());
    }

    for (LineChartSeries series : chartSeriesCollection) {

      if (states != null) {
        Optional<String> match = Arrays.stream(states.toUpperCase().split(","))
            .map(sym -> syms.get(sym))
            .filter(name -> name != null)
            .filter(name -> name.equals(series.getLabel()))
            .findAny();

        if (match.isPresent()) {
          regionGraph.addSeries(series);
          LOG.info("Adding: " + series.getLabel());
        }
      } else {
        regionGraph.addSeries(series);
      }

    }

  }


  public LineChartModel getGraph() {
    return regionGraph;
  }


}
