# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# 1. Compilación
# ---------------------------------------------------------------------------
FROM eclipse-temurin:26-jdk AS build

WORKDIR /build

# Descriptores primero, en su propia capa: mientras no cambien las
# dependencias, Docker reutiliza la descarga entre builds.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle gradle.properties ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src

# Sin tests: los ejecuta la integración continua, no el despliegue.
RUN ./gradlew --no-daemon bootJar -x test

# ---------------------------------------------------------------------------
# 2. Ejecución
# ---------------------------------------------------------------------------
FROM eclipse-temurin:26-jre

# curl solo para el HEALTHCHECK. La imagen jre no lo trae.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home /app app

WORKDIR /app

COPY --from=build --chown=app:app /build/build/libs/uncapitulomas.jar app.jar

# El volumen se monta aquí. Se crea con el dueño correcto porque el proceso no
# corre como root y no podría crearlo después.
RUN mkdir -p /var/lib/uncapitulomas/covers && chown -R app:app /var/lib/uncapitulomas

# Nunca como root: si aparece una ejecución remota, que al menos no sea con
# todos los permisos del contenedor.
USER app

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    COVERS_PATH=/var/lib/uncapitulomas/covers \
    TZ=Europe/Madrid \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

# Forma exec para que la JVM reciba SIGTERM y el apagado ordenado funcione.
ENTRYPOINT ["java", "-jar", "app.jar"]