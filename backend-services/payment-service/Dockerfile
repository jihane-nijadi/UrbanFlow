FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY pom.xml /app/
COPY src /app/src
RUN apk add --no-cache maven
RUN mvn -f /app/pom.xml -DskipTests package
EXPOSE 8085
CMD ["java","-jar","/app/target/payment-service-0.0.1-SNAPSHOT.jar"]
