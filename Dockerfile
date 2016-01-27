FROM java:8
ADD circle.yml app.yml
ADD build/libs/url-check-monitor-0.0.1.jar app.jar
RUN ls
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]