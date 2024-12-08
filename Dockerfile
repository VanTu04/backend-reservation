# Build stage
FROM maven:3-openjdk-17 AS build
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Sao chép file .war vào container
COPY --from=build /app/target/shopapp-0.0.1-SNAPSHOT.war /app/shopapp.war

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "shopapp.war"]
