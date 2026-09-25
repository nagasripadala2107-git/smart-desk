# ==========================================
# Stage 1: Build Environment (Java 26 JDK)
# ==========================================
FROM eclipse-temurin:26-jdk AS builder
WORKDIR /workspace

# Transparent proxy configuration for Docker Desktop environments
ARG JAVA_TOOL_OPTIONS="-Dhttps.proxyHost=http.docker.internal -Dhttps.proxyPort=3128 -Dhttp.proxyHost=http.docker.internal -Dhttp.proxyPort=3128"
ENV JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS}

# Configure Maven proxy for internal build resolution
RUN mkdir -p /root/.m2 && printf '%s\n' \
    '<settings>' \
    '  <proxies>' \
    '    <proxy>' \
    '      <id>docker-proxy-http</id>' \
    '      <active>true</active>' \
    '      <protocol>http</protocol>' \
    '      <host>http.docker.internal</host>' \
    '      <port>3128</port>' \
    '    </proxy>' \
    '    <proxy>' \
    '      <id>docker-proxy-https</id>' \
    '      <active>true</active>' \
    '      <protocol>https</protocol>' \
    '      <host>http.docker.internal</host>' \
    '      <port>3128</port>' \
    '    </proxy>' \
    '  </proxies>' \
    '</settings>' > /root/.m2/settings.xml

# Copy Maven Wrapper and POM first to leverage layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Normalize line endings and ensure executable permissions for Maven wrapper
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Resolve project dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy project source code
COPY src/ src/

# Build application JAR (skipping tests as per requirement)
RUN ./mvnw clean package -DskipTests -B

# Find and normalize the final runnable JAR
RUN cp target/smartdesk-backend-*.jar app.jar

# Compile a minimal, standalone Java HealthCheck utility for the minimal JRE runtime
RUN printf '%s\n' \
    'import java.net.URI;' \
    'import java.net.http.HttpClient;' \
    'import java.net.http.HttpRequest;' \
    'import java.net.http.HttpResponse;' \
    'import java.time.Duration;' \
    'public class HealthCheck {' \
    '    public static void main(String[] args) {' \
    '        try {' \
    '            HttpClient client = HttpClient.newBuilder()' \
    '                    .connectTimeout(Duration.ofSeconds(3))' \
    '                    .build();' \
    '            HttpRequest request = HttpRequest.newBuilder()' \
    '                    .uri(URI.create("http://localhost:8080/actuator/health"))' \
    '                    .timeout(Duration.ofSeconds(3))' \
    '                    .GET()' \
    '                    .build();' \
    '            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());' \
    '            System.exit((response.statusCode() == 200 && response.body().contains("\"UP\"")) ? 0 : 1);' \
    '        } catch (Exception e) {' \
    '            System.exit(1);' \
    '        }' \
    '    }' \
    '}' > HealthCheck.java && javac HealthCheck.java

# ==========================================
# Stage 2: Runtime Environment (Java 26 JRE)
# ==========================================
FROM eclipse-temurin:26-jre
WORKDIR /app

# Create a non-root system group and user
RUN groupadd -r appgroup && useradd -r -g appgroup -u 1001 appuser

# Copy application JAR and HealthCheck class from build stage
COPY --from=builder /workspace/app.jar /app/app.jar
COPY --from=builder /workspace/HealthCheck.class /app/HealthCheck.class

# Set ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose backend HTTP port
EXPOSE 8080

# Health check using the native Java Actuator health checker
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD java -cp /app HealthCheck || exit 1

# Launch the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
