package org.gpc4j.corona;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 *
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
  public static void main(String[] args) {

    /**
     Workaround for issue in RegionBean.sortCharts:

     java.lang.IllegalArgumentException: Comparison method violates its general contract!
     at java.base/java.util.TimSort.mergeLo(TimSort.java:781) ~[na:na]

     */
    System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

    SpringApplication.run(ApplicationMain.class, args);
  }

}
