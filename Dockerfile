# ═══════════════════════════════════════════
# Multi-stage Dockerfile for MediQueue
# Stage 1: Build with Maven
# Stage 2: Run with JRE 21
# ═══════════════════════════════════════════

# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml dependency:go-offline -B 2>/dev/null || true

# Copy source and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B && \
    mkdir -p target/dependency && \
    (cd target/dependency; jar -xf ../*.jar)

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

LABEL org.opencontainers.image.title="Hospital Queue Management System"
LABEL org.opencontainers.image.description="Real-Time Hospital Queue with JWT, WebSockets & Reports"
LABEL org.opencontainers.image.vendor="MediQueue"

# Non-root user for security
RUN addgroup -S mediqueue && adduser -S mediqueue -G mediqueue
USER mediqueue

# Copy layered jar for faster starts
ARG DEPENDENCY=/app/target/dependency
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib     ./BOOT-INF/lib
COPY --from=builder ${DEPENDENCY}/META-INF         ./META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes ./BOOT-INF/classes

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=70", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
