# Stage 1: Build the application using Maven
FROM maven:3-openjdk-17 AS build
WORKDIR /app

# Sao chép mã nguồn vào container
COPY . .

# Build ứng dụng và tạo file .jar
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM openjdk:17-jdk-slim
WORKDIR /app

# Sao chép file .jar vào container từ build stage
COPY --from=build /app/target/shopapp-0.0.1-SNAPSHOT.jar /app/shopapp.jar

# Mở cổng 8080
EXPOSE 8080

# Chạy ứng dụng Java
ENTRYPOINT ["java", "-jar", "shopapp.jar"]
