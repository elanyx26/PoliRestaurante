# ---- Compilación ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# maven.test.skip: no compila ni descarga dependencias de pruebas (el servidor de compilación es pequeño).
RUN mvn -B -ntp -q -Dmaven.test.skip=true package

# ---- Ejecución ----
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app

# Ajustes para servidores de 512 MB (Render gratis): memoria acotada, un solo recolector y JIT ligero.
ENV JAVA_OPTS="-Xmx300m -XX:MaxMetaspaceSize=150m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError" \
    SWAGGER_ENABLED=false \
    DB_POOL_SIZE=5

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
