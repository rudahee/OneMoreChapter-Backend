# Dependencias

Verificadas a 8 de septiembre de 2026.

## Versiones fijadas

| Artefacto | Versión | Nota |
|---|---|---|
| Spring Boot | 4.1.1 | agosto 2026, sobre Spring Framework 7 |
| springdoc-openapi | 3.1.1 | la serie 3.x es la de Boot 4; la 2.x es para Boot 3 |
| Bucket4j | 8.19.0 | mayo 2026 |
| io.spring.dependency-management | 1.1.7 | |

Todo lo demás lo gobierna el BOM de Spring Boot: MariaDB driver, Flyway,
Hibernate 7.4, Testcontainers, Micrometer, Lombok. No fijes esas versiones a
mano o acabarás con incompatibilidades sutiles.

Requisitos: Java 17 mínimo (el build usa 26), Gradle 9.x para que el
toolchain de Java 26 se resuelva.

## Qué aporta cada bloque

**Web.** `starter-web` y `starter-validation` son obvios. `starter-websocket`
es para el STOMP de la sincronización en tiempo real.

**Seguridad.** `starter-oauth2-resource-server` en lugar de jjwt. Aporta
`JwtEncoder` y `JwtDecoder` sobre Nimbus, que son las abstracciones que el
resto de Spring Security ya entiende; con jjwt tendrías que escribir el puente.

**Persistencia.** `flyway-mysql` no es opcional. Desde Flyway 10 el soporte por
motor va en módulos separados, y sin él Flyway arranca y falla diciendo que no
reconoce la base de datos. Es un fallo que despista bastante.

**Caché.** `starter-cache` da las anotaciones, `starter-data-redis` la
implementación. Ambas hacen falta.

**Test.** WireMock en vez de `MockRestServiceServer` porque hay que probar el
`RateLimitingInterceptor`, y para eso necesitas respuestas 429 reales con
cabecera `Retry-After`.

**`spring-boot-configuration-processor`.** Genera los metadatos de
`@ConfigurationProperties`, lo que da autocompletado del YAML en el IDE. Con el
bloque `metadata.providers.*` se nota.

## Lo que hay que revisar del código ya escrito

Boot 4 no es una actualización menor sobre lo que llevamos:

1. **Jackson 3.** Boot 4 lo trae por defecto. El paquete de databind pasó a
   `tools.jackson.databind`; las anotaciones (`@JsonProperty`) siguen en
   `com.fasterxml.jackson.annotation`. Si al compilar los proveedores de
   metadatos falla algún import, es esto.

2. **Spring Security 7.** El DSL encadenado con `.and()` ya no existe: todo es
   lambda. Lo tengo en cuenta al escribir el paquete `security`.

3. **Modularización de Boot 4.** Los jars se han dividido en unidades más
   pequeñas. Los starters que uso aquí siguen existiendo con el mismo nombre,
   pero si añades algo fuera de esta lista, comprueba el artefacto.

4. **Hibernate 7.** Cambia el comportamiento por defecto de algunas
   validaciones de esquema. Con `ddl-auto: validate` y Flyway al mando lo
   notarás en el primer arranque si hay discrepancias.

Si prefieres no comerte una migración mayor ahora mismo, la última 3.x fue la
3.5.16 (junio 2026), pero su ventana de soporte abierto ya terminó: solo 4.0 y
4.1 reciben parches. Para un proyecto nuevo no tiene sentido empezar en una
rama sin soporte.

## Wrapper

```bash
gradle wrapper --gradle-version 9.0
```

Y commitea `gradle/wrapper/`, `gradlew` y `gradlew.bat`.