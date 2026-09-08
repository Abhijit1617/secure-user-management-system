# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1: build
# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy the POM first so dependency resolution is cached across builds that
# only change application source, not the POM.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src src
RUN mvn -q -B clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2: runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --system controlplane && useradd --system --gid controlplane controlplane

WORKDIR /app

COPY --from=build /workspace/target/backend-control-plane.jar app.jar

RUN mkdir -p /app/storage /app/logs && chown -R controlplane:controlplane /app

USER controlplane

EXPOSE 8080

ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
