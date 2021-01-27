FROM openjdk:11

LABEL app.name="corona"

COPY target/Corona-*.jar /usr/src/corona.jar
WORKDIR                  /usr/src/

CMD ["java", "-jar", "corona.jar" ]


