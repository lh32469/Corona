FROM openjdk:11

LABEL app.name="corona"

COPY target/Corona-*.jar /usr/src/corona.jar
COPY COVID               /usr/src/COVID
WORKDIR                  /usr/src/

CMD ["java", "-jar", "corona.jar" ]


