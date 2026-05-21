package com.platform.proxy.command.querytranslator.adapters.inbound;

import com.platform.proxy.command.querytranslator.domain.dto.outbound.TranslatedQueryResponseDTO;
import com.platform.proxy.command.querytranslator.domain.event.QueryTranslatedEvent;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueryTranslatorIntegrationTest {

    private static MockWebServer downstream;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CapturingEventListener eventListener;

    @org.junit.jupiter.api.BeforeEach
    void extendTimeout() {
        // Allow for reactor-netty cold-start on the first downstream call.
        webTestClient = webTestClient.mutate()
                .responseTimeout(java.time.Duration.ofSeconds(30)).build();
    }

    @BeforeAll
    static void startServer() throws IOException {
        downstream = new MockWebServer();
        downstream.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        downstream.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("proxy.downstream.base-url", () -> "http://127.0.0.1:" + downstream.getPort() + "/");
        registry.add("proxy.downstream.id-field", () -> "metadata.uid");
        registry.add("proxy.downstream.timeout-ms", () -> "15000");
    }

    @Test
    void translatesFullFlowDedupingAcrossOrGroups() {
        eventListener.events.clear();
        // Two OR groups → two downstream calls. Overlapping uid "2" is deduped.
        downstream.enqueue(jsonItems("{\"metadata\":{\"uid\":\"1\"}},{\"metadata\":{\"uid\":\"2\"}}"));
        downstream.enqueue(jsonItems("{\"metadata\":{\"uid\":\"2\"}},{\"metadata\":{\"uid\":\"3\"}}"));

        TranslatedQueryResponseDTO body = webTestClient.get()
                .uri(uri -> uri.path("/translator/entities")
                        .queryParam("filter", "spec.type=image,spec.appId=CLAUT")
                        .queryParam("filter", "spec.available=true")
                        .queryParam("limit", "50")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(TranslatedQueryResponseDTO.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.getTotal()).isEqualTo(3);
        assertThat(body.isDegraded()).isFalse();

        await().untilAsserted(() ->
                assertThat(eventListener.events).hasSizeGreaterThanOrEqualTo(1));
        assertThat(eventListener.events.get(0).getResultCount()).isEqualTo(3);
    }

    @Test
    void returnsBadRequestForMalformedFilter() {
        webTestClient.get()
                .uri(uri -> uri.path("/translator/entities")
                        .queryParam("filter", "no-operator-here")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    private static MockResponse jsonItems(String items) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"items\":[" + items + "]}");
    }

    @TestConfiguration
    static class TestListenerConfig {
        @Bean
        CapturingEventListener capturingEventListener() {
            return new CapturingEventListener();
        }
    }

    static class CapturingEventListener {
        final List<QueryTranslatedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        void onEvent(QueryTranslatedEvent event) {
            events.add(event);
        }
    }
}
