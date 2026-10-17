# Multi-Architecture Dockerfile for PulseGuard-Z (supports linux/s390x & linux/amd64)
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build
COPY . .

RUN javac -d target/classes -sourcepath src/main/java \
    src/main/java/com/ibmz/pulseguard/*.java \
    src/main/java/com/ibmz/pulseguard/model/*.java \
    src/main/java/com/ibmz/pulseguard/engine/*.java \
    src/main/java/com/ibmz/pulseguard/service/*.java \
    src/main/java/com/ibmz/pulseguard/server/*.java

RUN mkdir -p target/classes/static && cp -r src/main/resources/static/* target/classes/static/ 2>/dev/null || true

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=builder /build/target/classes /app/classes

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+AlwaysPreTouch", "-cp", "/app/classes", "com.ibmz.pulseguard.Main", "8080"]
