FROM adoptopenjdk/openjdk11:alpine-jre

# ${PORT}
VOLUME /tmp
ADD target/*.jar app.jar

EXPOSE 9100
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /app.jar" ]
