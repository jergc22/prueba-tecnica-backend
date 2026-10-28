# Productos similares

API REST en Spring Boot que, dado un producto, devuelve el detalle de sus productos similares ordenados por similitud.

```
GET http://localhost:5000/product/{productId}/similar
```

- Contrato: [`similarProducts.yaml`](similarProducts.yaml)
- APIs existentes que consume: [`existingApis.yaml`](existingApis.yaml)
- Enunciado original: https://github.com/dalogax/backendDevTest

## Arranque rápido

Solo hace falta Docker (y `make`):

```bash
make up
```

Levanta la aplicación en el puerto 5000, los mocks del enunciado en el 3001, InfluxDB y Grafana.

```bash
curl http://localhost:5000/product/1/similar
make k6      # test de carga del enunciado
make down    # para todo
```

Resultados del test de carga en Grafana: http://localhost:3000/d/Le2Ku9NMk/k6-performance-test

`make help` lista todos los comandos. Sin `make`, los equivalentes son:

```bash
docker compose up -d --build app simulado influxdb grafana
docker compose run --rm k6 run scripts/test.js
docker compose down
```

En Docker Desktop puede ser necesario compartir la carpeta `shared` (Settings > Resources > File sharing).

### Tests

```bash
make test-docker   # dentro de Docker, sin Java instalado
make test          # en local, con JDK 21+
```

### Desarrollo en local

Con JDK 21+: `make mocks` para levantar solo los mocks y `make run` para arrancar la aplicación con el wrapper de Maven.

## Stack

- Java 21 y Spring Boot 4.1 (el enunciado sugiere 3.x, pero start.spring.io ya solo ofrece 4.x).
- Spring MVC con hilos virtuales.
- `RestClient` sobre el `HttpClient` del JDK.
- Caffeine con Spring Cache.
- JUnit 5, Mockito y WireMock.

## Arquitectura

```
controller  →  service  →  client (ProductClient)  →  APIs existentes
```

- **`SimilarProductsController`** traduce HTTP. `ApiExceptionHandler` convierte las excepciones en códigos de estado.
- **`SimilarProductsService`** orquesta: pide los ids similares, después los detalles en paralelo, y monta la respuesta.
- **`ProductClient`** es la interfaz hacia las APIs existentes y **`HttpProductClient`** su implementación. El servicio depende de la interfaz y no de `RestClient`, así que se prueba con un mock sin levantar Spring.
- **`ProductDetail`** es un `record` que valida el contrato al construirse.

## Decisiones

### Paralelismo con hilos virtuales

Los detalles se piden a la vez con `CompletableFuture` sobre `Executors.newVirtualThreadPerTaskExecutor()`. La lista se monta en el orden de `similarids`, no en el de llegada.

El trabajo es I/O bloqueante: esperar a otra API. Con hilos virtuales cada espera libera el hilo del sistema y el código sigue siendo imperativo y fácil de leer, sin pasar a WebFlux. No uso `parallelStream` porque se apoya en el `commonPool`, dimensionado para CPU, y bloquearlo con llamadas de red lo satura.

En serie, el producto 2 tardaría más de 6 s. En paralelo, lo que tarde el similar más lento hasta el timeout.

### Timeouts

Conexión 500 ms y lectura 1,5 s, configurables en `application.yml` (`products.api.*`).

Sin timeouts, el similar que tarda 50 s ocuparía una conexión por petición y con 200 usuarios el servicio se degradaría. Con 1,5 s pasa el similar lento de 1 s y se cortan los de 5 s y 50 s. Cada petición queda acotada a unos 1,5 s, aunque la respuesta llegue goteando.

### Si falla un similar: respuesta parcial

El contrato no dice qué hacer si un similar falla (404, 5xx, timeout o una respuesta que no cumple el contrato). Opto por **omitirlo, registrarlo y devolver el resto**. Una lista de recomendaciones incompleta sigue siendo útil, y una dependencia lenta o rota no debería tumbar la respuesta entera.

La alternativa es responder con error (502/504) en cuanto falle uno. Es más estricta, pero peor para quien consume la API.

Solo se omiten los fallos esperados de la API. Un error de programación no se esconde como "similar omitido".

### Si falla la consulta de ids

- Producto inexistente (`similarids` devuelve 404): **404**, como pide el contrato.
- 5xx, timeout, error de red o cuerpo vacío: **502 Bad Gateway**, porque falla un servicio del que depende. No está en el contrato, así que lo dejo documentado aquí.

### Respuestas inesperadas de las APIs existentes

- Los mocks devuelven los ids como números (`[2,3,4]`) y el contrato los quiere como `string`: se deserializan como `List<String>`.
- Los ids repetidos se eliminan (el contrato exige `uniqueItems`), manteniendo el orden. Los ids nulos o vacíos se descartan.
- Un detalle sin precio o sin disponibilidad, o con `id` o `name` vacíos, no cumple el contrato: no se devuelve y se trata como un similar fallido.
- El precio es `BigDecimal` para no perder precisión.

### Caché

Caffeine vía `@Cacheable` para los detalles (`products`) y los ids (`similarIds`), con expiración de 60 s y un máximo de 10.000 entradas. Los mismos productos se piden miles de veces y cambian poco.

**Solo se cachean respuestas correctas.** Spring no cachea las excepciones, así que un 404, un 500 o un timeout se vuelven a intentar en la siguiente petición y un fallo puntual no queda "congelado" 60 s.

No uso `sync = true`. Combinado con no cachear fallos, serializaba las peticiones concurrentes a un similar lento: cada una esperaba el timeout completo detrás de la anterior. Hay un test que lo cubre. El coste es que, con la caché fría, varias peticiones simultáneas pueden repetir la misma llamada.

## Resultados del test de carga

Test k6 del enunciado: escenarios normal, notFound, error, slow y verySlow, con 200 usuarios virtuales durante 10 s cada uno.

| | Sin caché | Con caché |
|---|---|---|
| Peticiones | 10.135 (~167/s) | 13.556 (~225/s) |
| Duración media | 508 ms | 247 ms |
| Mediana | 264 ms | 13 ms |
| p90 / p95 | 1,55 s / 1,65 s | 1,50 s / 1,51 s |

La mediana baja de 264 ms a 13 ms. El p90/p95 se queda en ~1,5 s por los escenarios slow y verySlow: sus similares lentos agotan el timeout en cada petición, porque los fallos no se cachean.

## Tests

20 tests:

- **Unitarios del servicio:** orden aunque las respuestas lleguen desordenadas, omisión de fallos, ids duplicados, propagación de 404/502, y que un error inesperado no se esconda.
- **De integración:** levantan la aplicación completa contra WireMock. Cubren orden, 404, 502, similares con 404/500/timeout, respuestas que no cumplen el contrato, caché de éxitos, que los fallos no se cacheen, y peticiones concurrentes a un similar lento que no se encolan.

## Mejoras futuras

- **Circuit breaker y bulkhead (Resilience4j):** dejar de llamar a una API que está fallando de forma sostenida y limitar las llamadas concurrentes a cada una.
- **Caché negativa corta (unos segundos):** para que los similares que hacen timeout no penalicen cada petición. Es un compromiso entre latencia y frescura.
- **Observabilidad:** Actuator y Micrometer (latencia y errores por API, aciertos de caché, similares omitidos) y un id de correlación en los logs.
- **Varias instancias:** caché distribuida (por ejemplo Redis) y una estrategia de invalidación.
