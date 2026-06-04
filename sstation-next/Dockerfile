# syntax=docker/dockerfile:1
# TC-113: multi-stage image — build JAR with JDK 21, run with JRE 21 Alpine.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src

# Strip CRLF when the repo is checked out on Windows — otherwise Alpine cannot exec gradlew.
RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS run

RUN apk add --no-cache wget \
    && addgroup -g 1001 sstation \
    && adduser -u 1001 -G sstation -D sstation

WORKDIR /app

COPY --from=build /workspace/build/libs/sstation-next-0.0.1-SNAPSHOT.jar app.jar

USER sstation

EXPOSE 8080

# Sized for small hosts (e.g. AWS t3.micro) when only the app container runs.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -q -O - http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
