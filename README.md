# Corona

Spring Boot JSF App for viewing data.  

Takes Daily data from this repo:

    https://github.com/CSSEGISandData/COVID-19

and displays number of cumulative cases by day for each US State.

Build:

    $ mvn clean package

Run:

    $ java -jar target/Corona-1.0.0.jar

Access via this path for top 15 states:

   http://www.gpc4j.org/corona/region.xhtml

Or access via this path for specific states:

   http://www.gpc4j.org/corona/region.xhtml?states=or,wa,ca


