# ============ BUILD STAGE ============
FROM eclipse-temurin:18-jdk-alpine AS builder

RUN apk add --no-cache maven

WORKDIR /app

# Cache dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source
COPY src ./src

# Build
RUN mvn clean package -DskipTests -B

# ============ RUNTIME STAGE ============
FROM eclipse-temurin:18-jdk-alpine

# Install tini + curl for healthcheck
RUN apk add --no-cache tini curl

WORKDIR /app

# Explicit JAR name
COPY --from=builder /app/target/*.jar /app/app.jar
# JVM options
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -Xms256m -Xmx512m -XX:+ExitOnOutOfMemoryError"

EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["tini", "--"]
CMD ["java", "-jar", "app.jar"]