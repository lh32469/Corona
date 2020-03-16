FROM openjdk:11

COPY target/Corona-*.jar /usr/src/corona.jar
COPY COVID               /usr/src/COVID
WORKDIR                  /usr/src/

CMD ["java", "-jar", "corona.jar" ]


