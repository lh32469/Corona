package org.gpc4j.corona.raven;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import java.io.IOException;

public class UpdateDataTest {


  @Test
  public void convertRecord() throws IOException {

    CSVParser parser = CSVParser.parse("reader", CSVFormat.DEFAULT);
    CSVRecord record = parser.getRecords().get(0);
    System.out.println("record = " + record);

  }



}
