# ============ BUILD STAGE ============
FROM eclipse-temurin:18-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

WORKDIR /app

# Copy only pom.xml first → leverage Docker cache
COPY pom.xml ./
# Download dependencies (cache layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build JAR (skip tests)
RUN mvn clean package -DskipTests -B

# ============ RUNTIME STAGE ============
FROM eclipse-temurin:18-jre-alpine

# Tini for signal handling (zombie process prevention)
RUN apk add --no-cache tini curl

WORKDIR /app

# Copy ONLY the final JAR (explicit name to avoid conflicts)
COPY --from=builder /app/target/*.jar /app/app.jar

# JVM tuning for low memory, fast startup
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -Xms256m -Xmx512m -XX:+ExitOnOutOfMemoryError"

# Expose port
EXPOSE 8082

# Health check using actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Use Tini as entrypoint
ENTRYPOINT ["tini", "--"]
CMD ["java", "-jar", "app.jar"]