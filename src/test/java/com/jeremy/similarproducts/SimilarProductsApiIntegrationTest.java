package com.jeremy.similarproducts;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Levanta la aplicación completa contra un WireMock que hace de API existente,
 * simulando los mismos casos que los mocks del enunciado: retrasos, 404 y 500.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"products.api.read-timeout=500ms"})
class SimilarProductsApiIntegrationTest {

    private static final WireMockServer wireMock = new WireMockServer(0);

    @LocalServerPort
    private int port;

    @Autowired
    private CacheManager cacheManager;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("products.api.base-url", wireMock::baseUrl);
    }

    @BeforeEach
    void resetState() {
        wireMock.resetAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void returnsSimilarProductsInOrder() throws Exception {
        stubIds("1", "[2,3]");
        stubProduct("2", "Dress", "19.99", true);
        stubProduct("3", "Blazer", "29.99", false);

        HttpResponse<String> response = getSimilar("1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(
                "[{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99,\"availability\":true},"
                        + "{\"id\":\"3\",\"name\":\"Blazer\",\"price\":29.99,\"availability\":false}]");
    }

    @Test
    void returns404WhenTheProductDoesNotExist() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/product/99/similarids")).willReturn(aResponse().withStatus(404)));

        assertThat(getSimilar("99").statusCode()).isEqualTo(404);
    }

    @Test
    void omitsSimilarProductsThatReturn404Or500() throws Exception {
        stubIds("4", "[1,5,6]");
        stubProduct("1", "Shirt", "9.99", true);
        wireMock.stubFor(get(urlEqualTo("/product/5")).willReturn(aResponse().withStatus(404)));
        wireMock.stubFor(get(urlEqualTo("/product/6")).willReturn(aResponse().withStatus(500)));

        HttpResponse<String> response = getSimilar("4");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[{\"id\":\"1\",\"name\":\"Shirt\",\"price\":9.99,\"availability\":true}]");
    }

    @Test
    void omitsSimilarProductsThatExceedTheTimeout() throws Exception {
        stubIds("2", "[3,1000]");
        stubProduct("3", "Blazer", "29.99", false);
        wireMock.stubFor(get(urlEqualTo("/product/1000")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"1000\",\"name\":\"Slow\",\"price\":1,\"availability\":true}")
                .withFixedDelay(3000)));

        long start = System.nanoTime();
        HttpResponse<String> response = getSimilar("2");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"id\":\"3\"").doesNotContain("1000");
        assertThat(elapsedMs).isLessThan(2500);
    }

    @Test
    void returns502WhenTheSimilarIdsApiFails() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/product/7/similarids")).willReturn(aResponse().withStatus(500)));

        assertThat(getSimilar("7").statusCode()).isEqualTo(502);
    }

    @Test
    void cachesSuccessfulResponses() throws Exception {
        stubIds("1", "[2]");
        stubProduct("2", "Dress", "19.99", true);

        getSimilar("1");
        getSimilar("1");

        wireMock.verify(1, getRequestedFor(urlEqualTo("/product/1/similarids")));
        wireMock.verify(1, getRequestedFor(urlEqualTo("/product/2")));
    }

    @Test
    void doesNotCacheFailures() throws Exception {
        stubIds("4", "[6]");
        wireMock.stubFor(get(urlEqualTo("/product/6")).willReturn(aResponse().withStatus(500)));

        getSimilar("4");
        getSimilar("4");

        wireMock.verify(2, getRequestedFor(urlEqualTo("/product/6")));
    }

    @Test
    void doesNotRepeatProductsWhenSimilarIdsContainDuplicates() throws Exception {
        stubIds("1", "[2,2,3,2]");
        stubProduct("2", "Dress", "19.99", true);
        stubProduct("3", "Blazer", "29.99", false);

        HttpResponse<String> response = getSimilar("1");

        assertThat(response.body()).isEqualTo(
                "[{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99,\"availability\":true},"
                        + "{\"id\":\"3\",\"name\":\"Blazer\",\"price\":29.99,\"availability\":false}]");
        wireMock.verify(1, getRequestedFor(urlEqualTo("/product/2")));
    }

    @Test
    void ignoresNullOrBlankIdsInSimilarIds() throws Exception {
        stubIds("1", "[2,null,\"\"]");
        stubProduct("2", "Dress", "19.99", true);

        HttpResponse<String> response = getSimilar("1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"id\":\"2\"");
        wireMock.verify(0, getRequestedFor(urlEqualTo("/product/null")));
    }

    @Test
    void returns502WhenSimilarIdsBodyIsEmpty() throws Exception {
        stubIds("1", "null");

        assertThat(getSimilar("1").statusCode()).isEqualTo(502);
    }

    @Test
    void omitsSimilarProductsThatDoNotFulfilTheContract() throws Exception {
        stubIds("1", "[2,3,4,5]");
        stubProduct("2", "Dress", "19.99", true);
        stubRaw("3", "{\"id\":\"3\",\"name\":\"NoPrice\",\"availability\":true}");
        stubRaw("4", "{\"id\":\"4\",\"name\":\"\",\"price\":1,\"availability\":true}");
        stubRaw("5", "null");

        HttpResponse<String> response = getSimilar("1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99,\"availability\":true}]");
    }

    @Test
    void concurrentRequestsToASlowSimilarDoNotQueueUpBehindEachOther() throws Exception {
        stubIds("2", "[3,1000]");
        stubProduct("3", "Blazer", "29.99", false);
        wireMock.stubFor(get(urlEqualTo("/product/1000")).willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(3000)));

        int requests = 10;
        long start = System.nanoTime();
        try (ExecutorService callers = Executors.newFixedThreadPool(requests)) {
            List<Future<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(callers.submit(() -> getSimilar("2")));
            }
            for (Future<HttpResponse<String>> future : futures) {
                assertThat(future.get().statusCode()).isEqualTo(200);
            }
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Si se encolaran, 10 peticiones x 500 ms de timeout superarían los 5 s.
        assertThat(elapsedMs).isLessThan(2000);
    }

    private HttpResponse<String> getSimilar(String productId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/product/" + productId + "/similar")).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void stubIds(String productId, String jsonIds) {
        wireMock.stubFor(get(urlEqualTo("/product/" + productId + "/similarids")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(jsonIds)));
    }

    private void stubRaw(String id, String json) {
        wireMock.stubFor(get(urlEqualTo("/product/" + id)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(json)));
    }

    private void stubProduct(String id, String name, String price, boolean availability) {
        wireMock.stubFor(get(urlEqualTo("/product/" + id)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"price\":" + price
                        + ",\"availability\":" + availability + "}")));
    }
}
