# Stage 1: Build
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run with Elastic APM agent
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Download Elastic APM Java Agent
RUN apk add --no-cache curl && \
    curl -sSL https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/1.48.1/elastic-apm-agent-1.48.1.jar \
    -o /app/elastic-apm-agent.jar

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-javaagent:/app/elastic-apm-agent.jar", \
  "-Delastic.apm.service_name=spring-elk-demo", \
  "-Delastic.apm.server_url=http://apm-server:8200", \
  "-Delastic.apm.environment=docker", \
  "-Delastic.apm.log_level=INFO", \
  "-Delastic.apm.application_packages=com.demo", \
  "-jar", "app.jar"]
