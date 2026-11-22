# LabLink container image. Built in two stages so the final image holds only a Java runtime and
# the app's .jar - no Maven, no source code, no compiler. See docs/containerization.md.

# ---------- Stage 1: build the .jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy only the POM first and download dependencies. Docker caches this layer, so later builds
# skip the download unless pom.xml changed.
COPY pom.xml .
COPY config ./config
RUN mvn -B -q dependency:go-offline

# Now the source. Tests and lint are skipped here because CI already ran them on this commit.
COPY src ./src
RUN mvn -B -q package -DskipTests -Dcheckstyle.skip=true -Djacoco.skip=true

# ---------- Stage 2: the image that actually runs ----------
FROM eclipse-temurin:17-jre

# curl is used by the health check below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Never run the app as root inside the container.
RUN groupadd --system lablink && useradd --system --gid lablink --no-create-home lablink

WORKDIR /app
COPY --from=build /workspace/target/lablink-*.jar app.jar
USER lablink

# Defaults; override with -e or in docker-compose.yml.
# TZ: the app compares reservation times with LocalDateTime.now(), so the container must use
# campus time. Containers default to UTC, which would put "now" five hours ahead in Kentucky.
# MaxRAMPercentage lets the JVM size its heap from the container's memory limit.
ENV TZ=America/New_York \
    SPRING_PROFILES_ACTIVE=mysql \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Docker marks the container "unhealthy" if the app stops answering.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

# "exec" makes Java process 1, so it receives Docker's stop signal and shuts down gracefully.
ENTRYPOINT ["sh", "-c", "exec java -Duser.timezone=$TZ $JAVA_OPTS -jar /app/app.jar"]
