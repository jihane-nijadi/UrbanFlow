FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy pom and sources, then build the application
COPY pom.xml ./
COPY src ./src

# Download dependencies and build
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for running the app
RUN addgroup -S spring && adduser -S spring -G spring || true
USER spring:spring

# Copy the built jar from the build stage
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
