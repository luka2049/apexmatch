FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="apexmatch"

WORKDIR /app

COPY apexmatch-gateway-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=10"
ENV SPRING_PROFILES_ACTIVE="prod"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
