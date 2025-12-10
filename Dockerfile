FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/eventsProject-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
