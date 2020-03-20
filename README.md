# Corona

Spring Boot JSF App for viewing data.  

Running here:  http://www.gpc4j.org/corona

Takes Daily data from this repo:

    https://github.com/CSSEGISandData/COVID-19

and displays number of cumulative cases by day for each US State.

Build:

    $ mvn clean package

Run:

    $ git clone https://github.com/CSSEGISandData/COVID-19 COVID

    $ java -jar target/Corona-1.0.0.jar
 
Access local copy:

    http://localhost:8080/corona


