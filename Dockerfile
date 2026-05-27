FROM gradle:8-jdk21-alpine AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle :core:buildFatJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/core/build/libs/core-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx1g", "-jar", "app.jar"]
