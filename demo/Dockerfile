# ============================================================
# Stage 1: Build Spring Boot JAR
# ============================================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom.xml trước để cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source và build (bỏ qua test để build nhanh hơn)
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================================
# Stage 2: Runtime image nhẹ hơn
# ============================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Tạo user non-root để bảo mật
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy JAR từ stage build
COPY --from=builder /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Tạo thư mục uploads và gán quyền
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
