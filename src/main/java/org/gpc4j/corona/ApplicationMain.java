package org.gpc4j.corona;

import org.gpc4j.corona.raven.UpdateData;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * @author Lyle T Harris (lyle.harris@gmail.com)
 */
@SpringBootApplication
//@ComponentScan({"org.gpc4j.corona.beans"})

//@RestController
public class ApplicationMain {

  //  @Autowired()
//  HystrixTestCommand2 cmd;
//
//  @Bean
//  public RestTemplate rest(RestTemplateBuilder builder) {
//    return builder.build();
//  }
//
//  @RequestMapping("/greet")
//  public String showGreeting() {
//    return cmd.get(this.toString());
//  }
  public static void main(String[] args) throws IOException {
    
    // Add hostname to Mapped Diagnostic Context for Logback XML file variables.
    MDC.put("hostname", InetAddress.getLocalHost().getHostName());

    /**
     Workaround for issue in RegionBean.sortCharts:

     java.lang.IllegalArgumentException: Comparison method violates its general contract!
     at java.base/java.util.TimSort.mergeLo(TimSort.java:781) ~[na:na]

     */
    System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

    System.out.println("Arrays.toString(args) = " + Arrays.toString(args));

    if (Arrays.asList(args).contains("update")) {
      UpdateData.main(args);
    } else {
      SpringApplication.run(ApplicationMain.class, args);
    }
  }

}
