# Build stage
FROM gradle:9.3.1-jdk17 AS builder
WORKDIR /workspace
COPY . .
RUN gradle clean bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

