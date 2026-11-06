FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --gid 10001 sewain && useradd --uid 10001 --gid sewain --no-create-home sewain \
    && mkdir -p data upload && chown -R sewain:sewain /app
COPY --from=build --chown=sewain:sewain /workspace/build/libs/*.jar /app/app.jar
USER sewain
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
