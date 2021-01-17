package org.gpc4j.corona.raven;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.OrderingType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.util.Strings;
import org.gpc4j.corona.States;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UpdateData {

  /**
   * Location of cloned COVID-19 Git data repository.
   */
  static final String repoDir = "csse_covid_19_data/csse_covid_19_daily_reports";
  private static DocumentStore docStore;

  static Logger LOG = LoggerFactory.getLogger(UpdateData.class);


  static final Function<Path, List<CSVRecord>> getRecords =
      path -> {
        LOG.info("Loading Records: " + path);
        try {
          FileReader reader = new FileReader(path.toFile());
          CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
          return parser.getRecords().stream()
              .filter(record -> "US".equals(record.get("Country_Region")))
              .collect(Collectors.toList());
        } catch (NumberFormatException | IOException ex) {
          return Collections.emptyList();
        }
      };

  static final Function<CSVRecord, CountyEntry> convertRecord =
      record -> {
        try {
          CountyEntry entry = new CountyEntry();
          entry.setName(record.get("Admin2"));
          entry.setState(record.get("Province_State"));
          String lastUpdate = record.get("Last_Update");
          entry.setDay(lastUpdate.split(" ")[0].trim());
          entry.setDeaths(Integer.parseInt(record.get("Deaths")));
          entry.setCumulative(Integer.parseInt(record.get("Confirmed")));
          return entry;
        } catch (NumberFormatException ex) {
          LOG.warn(ex.toString());
          return new CountyEntry();
        }
      };

  static final Function<List<CSVRecord>, List<CountyEntry>> convertRecords =
      records -> {
        try {
          return records.parallelStream()
              .map(convertRecord)
              .collect(Collectors.toList());
        } catch (NumberFormatException ex) {
          return Collections.emptyList();
        }
      };


  static final Consumer<CountyEntry> storeEntry =
      county -> {
        try (IDocumentSession session = docStore.openSession()) {
          LOG.info("Storing: " + county);
          if (Strings.isNotEmpty(county.getState())) {
            String stateSymbol = States.SYMBOLS.get(county.getState().trim());
            final String id = county.getDay().trim() + "." + stateSymbol;
            StateEntry state = session.load(StateEntry.class, id);
            if (null == state) {
              state = new StateEntry();
              state.setState(county.getState());
              state.setSymbol(States.SYMBOLS.get(county.getState()));
              state.setDay(county.getDay());
            }
            // Null out redundant data.
            String countyName = county.getName();
            county.setState(null);
            county.setName(null);
            county.setDay(null);
            state.getCounties().put(countyName, county);
            state.update();
            session.store(state, id);
            session.saveChanges();
          }
        }
      };

  static final Consumer<List<CountyEntry>> storeEntries =
      listOfEntries -> {
        listOfEntries.forEach(storeEntry);
      };


  public static void main(String[] args) throws IOException {
    System.out.println("Loading " + repoDir);

    docStore = new DocumentStore("http://dell-4290.local:5050", "Corona");
    docStore.initialize();

    LocalDate now = LocalDate.now();

    Files.list(Path.of(repoDir))
        .filter(file -> file.getFileName().toString().endsWith(".csv"))
        .filter(file -> file.getFileName().toString().contains(String.valueOf(now.getYear())))
        .filter(file -> file.getFileName().toString().contains(String.valueOf(now.getMonthValue())))
        .filter(file -> file.getFileName().toString().contains(String.valueOf(now.getDayOfMonth() - 1)))
        .limit(1)
        .map(getRecords)
        .map(convertRecords)
        .forEach(storeEntries);

    docStore.close();

    top();
  }


  public static void top() {
    docStore = new DocumentStore("http://dell-4290.local:5050", "Corona");
    docStore.initialize();

    try (IDocumentSession session = docStore.openSession()) {

      List<StateEntry> top = session.query(StateEntry.class)
          .whereEquals("day", "2021-01-15")
          .orderBy("cumulative", OrderingType.LONG)
          .take(1)
          .toList();

      System.out.println("top = " + top);
      System.out.println("top.get(0).getState() = " + top.get(0).getState());
      System.out.println("top.get(0).getCumulative() = " + top.get(0).getCumulative());
      docStore.close();

    }

  }
}
