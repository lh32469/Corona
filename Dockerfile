from openjdk:11

COPY target/Corona-*.jar /usr/src/corona.jar
WORKDIR                  /usr/src/

EXPOSE 8080

CMD ["java", "-jar", "corona.jar" ]


