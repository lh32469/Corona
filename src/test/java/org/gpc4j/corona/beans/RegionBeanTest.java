package org.gpc4j.corona.beans;

import org.gpc4j.corona.States;
import org.gpc4j.corona.raven.StateEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.gpc4j.corona.beans.RegionBean.DTF;

public class RegionBeanTest {

  RavenBean ravenBean;
  DataBean dataBean;
  RegionBean regionBean;
  List<StateEntry> theData;

  final static private Logger LOG
      = LoggerFactory.getLogger(RegionBeanTest.class);


  @Before
  public void setup() {
    ravenBean = new RavenBean();
    dataBean = new DataBean();
    dataBean.postConstruct();

    regionBean = new RegionBean();
    regionBean.ravenBean = ravenBean;
    regionBean.dataBean = dataBean;

    theData = ravenBean.getSession().query(StateEntry.class)
        .whereIn("day", Arrays.asList("2021-01-22", "2021-01-23"))
        .whereNotEquals("symbol", null)
//        .whereIn("symbol", new LinkedList<>(States.NAME_TO_SYMBOL.values()))
        .toList();

    LOG.info("Number of Data Entries: " + theData.size());
//    LOG.info("theData = " + theData);
  }

  @Test
  public void test1() {
    LocalDate day = LocalDate.of(2021, 01, 23);
    Map<String, Float> rates = regionBean.getTheRates(day, Arrays.asList("OR"), theData);
    Assert.assertTrue(rates.containsKey("OR"));
    Assert.assertEquals(20.7, rates.get("OR"), 0.05);
  }

  @Test
  public void test2() {
    LocalDate day = LocalDate.of(2021, 01, 23);
    Map<String, Float> rates = regionBean.getTheRates(day, States.ALL_SYMBOLS, theData);
    Assert.assertTrue(rates.containsKey("OR"));
    Assert.assertEquals(20.7, rates.get("OR"), 0.05);


    List<String> top10 = rates.entrySet().stream()
        .sorted((e1, e2) -> Math.round(e2.getValue() - e1.getValue()))
        .limit(10)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());


    LOG.info("top10 = " + top10);
  }


  @Test
  public void todayOR() {
    LocalDate now = LocalDate.now();

    // Needs to be List of Objects for .whereIn clause
    List<Object> days = Arrays.asList(
        DTF.format(now.minusDays(0)),
        DTF.format(now.minusDays(1)));

    List<StateEntry> recentData = ravenBean.getSession().query(StateEntry.class)
        .whereIn("day", days)
        .whereNotEquals("symbol", null)
        .toList();

    Map<String, Float> rates = regionBean.getTheRates(now, Arrays.asList("OR"), recentData);
    LOG.info("rates = " + rates);
  }


}
