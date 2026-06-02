FROM gradle:8-jdk21-alpine AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle :root:buildFatJar --no-daemon
# Fail the build early if the batch entry point didn't make it into the fat jar,
# rather than shipping an image that ClassNotFounds at runtime.
RUN jar tf root/build/libs/root-all.jar | grep -q '^com/salles/root/ScraperJobKt\.class$' \
    || (echo "ERROR: com.salles.root.ScraperJobKt missing from root-all.jar" >&2 && exit 1)

# Playwright's official Java image: bundles a JDK plus all browsers and their
# system dependencies, version-matched to the playwright lib in libs.versions.toml.
FROM mcr.microsoft.com/playwright/java:v1.60.0-noble
WORKDIR /app
COPY --from=build /app/root/build/libs/root-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx1g", "-jar", "app.jar"]
