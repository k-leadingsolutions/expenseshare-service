# syntax=docker/dockerfile:1.4
# ===== Build Stage =====
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only dependency-related files first for caching
COPY pom.xml .

# Download dependencies using a persistent cache (BuildKit feature)
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline

# Now copy the source (cache will only be invalidated if code changes)
COPY src ./src

# Build the app with cache for .m2
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests spring-boot:repackage

# ===== Runtime Stage =====
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a non-root user and switch to it
RUN adduser --system --uid 1001 appuser
USER appuser

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]