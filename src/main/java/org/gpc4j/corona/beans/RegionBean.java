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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  @Inject
  private DataBean dataBean;

  @PostConstruct
  public void postConstruct() {

    Map<String, String> params = FacesContext.getCurrentInstance().
        getExternalContext().getRequestParameterMap();
    final String states = params.get("states");

    regionGraph = new LineChartModel();
    regionGraph.setTitle("Cumulative Cases by State");
    regionGraph.setLegendPosition("n");

    Axis xAxis = regionGraph.getAxis(AxisType.X);
    xAxis.setLabel("# Days");
    xAxis.setMin(0);

    Axis yAxis = regionGraph.getAxis(AxisType.Y);
    yAxis.setLabel("# Cases");
    yAxis.setMin(0);

    List<LineChartSeries> chartSeriesCollection;

    /*
     * By default, only show top 15 States.
     */
    if (states == null) {
      chartSeriesCollection = dataBean.getLineChartSeries()
          .limit(15)
          .collect(Collectors.toList());
    } else {
      // Convert to uppercase and final for lambda
      final String states2 = states.toUpperCase();
      chartSeriesCollection = dataBean.getLineChartSeries()
          .filter(cs -> {
            String symbol = syms.get(cs.getLabel());
            return symbol != null && states2.contains(symbol);
          })
          .collect(Collectors.toList());
    }

    for (LineChartSeries series : chartSeriesCollection) {
      regionGraph.addSeries(series);
    }

  }

  public LineChartModel getGraph() {
    return regionGraph;
  }


}
