# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the executable jar ----
FROM eclipse-temurin:26-jdk AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached while only the sources change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run in CI (`./mvnw verify`); the integration tests need Docker, which isn't available here.
RUN ./mvnw -B -q package -DskipTests \
	&& cp target/*.jar app.jar \
	&& java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---- Runtime stage: JRE only, non-root user ----
FROM eclipse-temurin:26-jre
WORKDIR /app

RUN groupadd --system micita && useradd --system --gid micita --no-create-home micita

# Spring Boot layers, from least to most frequently changed.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER micita

# prod reads DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, JWT_EXPIRATION,
# JWT_REFRESH_EXPIRATION, BOOTSTRAP_USERNAME and BOOTSTRAP_PASSWORD from the environment.
ENV SPRING_PROFILES_ACTIVE=prod \
	JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
