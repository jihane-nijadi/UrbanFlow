FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Use the Maven wrapper included in the repository
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src

RUN chmod +x mvnw && ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
