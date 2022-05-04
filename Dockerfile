FROM maven:3.8.5-jdk-11-slim AS build

WORKDIR /build
COPY ./ .
RUN mvn package

FROM openjdk:11 AS final
EXPOSE 8080
ENTRYPOINT ["java", "-Djsee.enableSNIExtension=false", "-jar", "gtfs-realtime-validator-webapp-1.0.0-SNAPSHOT.jar"]
CMD []
WORKDIR /app
COPY --from=build /build/gtfs-realtime-validator-webapp/target/ .
