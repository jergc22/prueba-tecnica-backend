# Prueba técnica backend - Productos similares

API REST que devuelve el detalle de los productos similares a uno dado
(`GET /product/{productId}/similar`, puerto 5000). Contrato en `similarProducts.yaml`.
Enunciado original: https://github.com/dalogax/backendDevTest

## Stack
- Java 21, Spring Boot 4.1.2 (el enunciado sugería 3.x, pero start.spring.io ya solo ofrece 4.x), Maven.
- Spring MVC con virtual threads (`spring.threads.virtual.enabled=true`).

## Cómo ejecutar
Requisitos: JDK 21+ y Docker.

1. Mocks y herramientas de carga:
   ```
   docker compose up -d simulado influxdb grafana
   ```
   Comprobación: http://localhost:3001/product/1/similarids
2. Aplicación:
   ```
   ./mvnw spring-boot:run
   ```
3. Test de carga (k6), resultados en http://localhost:3000/d/Le2Ku9NMk/k6-performance-test:
   ```
   docker compose run --rm k6 run scripts/test.js
   ```

## Decisiones
_Se irán documentando por hito._
