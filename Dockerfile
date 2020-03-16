from openjdk:11

COPY target/Corona-*.jar /usr/src/corona.jar
COPY COVID               /usr/src/
WORKDIR                  /usr/src/

CMD ["java", "-jar", "corona.jar" ]


