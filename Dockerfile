FROM gradle:8-jdk21-alpine AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle :root:buildFatJar --no-daemon

# Playwright's official Java image: bundles a JDK plus all browsers and their
# system dependencies, version-matched to the playwright lib in libs.versions.toml.
FROM mcr.microsoft.com/playwright/java:v1.60.0-noble
WORKDIR /app
COPY --from=build /app/root/build/libs/root-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx1g", "-jar", "app.jar"]
