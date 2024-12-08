# Sử dụng OpenJDK 17 làm base image
FROM openjdk:17 AS base

# Cài đặt Maven 3.9.8
RUN apt-get update && apt-get install -y wget \
    && wget https://dlcdn.apache.org/maven/maven-3/3.9.8/binaries/apache-maven-3.9.8-bin.tar.gz \
    && tar -xvzf apache-maven-3.9.8-bin.tar.gz -C /opt \
    && ln -s /opt/apache-maven-3.9.8/bin/mvn /usr/bin/mvn

# Kiểm tra phiên bản Maven
RUN mvn -version

# Đặt thư mục làm việc
WORKDIR /app

# Copy dự án vào container
COPY . .

# Chạy Maven build (nếu cần)
RUN mvn clean package

# Chạy ứng dụng của bạn (thay đổi theo file .war hoặc .jar của bạn)
CMD ["java", "-jar", "/app/target/your-app.jar"]
