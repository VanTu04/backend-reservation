FROM maven:3.9.8-openjdk-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package

FROM openjdk:17
WORKDIR /app
COPY --from=builder /app/target/DrComputer-0.0.1-SNAPSHOT.war /app/target/
CMD ["java", "-jar", "/app/target/DrComputer-0.0.1-SNAPSHOT.war"]
