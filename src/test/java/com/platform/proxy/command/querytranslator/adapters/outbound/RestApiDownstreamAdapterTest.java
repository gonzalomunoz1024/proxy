package com.platform.proxy.command.querytranslator.adapters.outbound;

import com.platform.proxy.core.credentials.DownstreamCredentialProvider;
import io.netty.resolver.DefaultAddressResolverGroup;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RestApiDownstreamAdapterTest {

    private MockWebServer server;
    private RestApiDownstreamAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        HttpClient httpClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE);
        WebClient client = WebClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getPort())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        // Generous per-call timeout so reactor-netty cold-start init does not
        // trip the timeout on the very first request in CI/daemon environments.
        adapter = new RestApiDownstreamAdapter(
                client, new DownstreamCredentialProvider(), "/resources", 15000, 3, 10);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void buildsQueryAndParsesItems() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"items\":[{\"metadata\":{\"uid\":\"1\"}}]}"));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("spec.type", "image");

        StepVerifier.create(adapter.query(params, null))
                .assertNext(item -> assertThat(item).containsKey("metadata"))
                .verifyComplete();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("/resources");
        assertThat(request.getPath()).contains("spec.type=image");
    }

    @Test
    void forwardsAuthorizationHeader() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"items\":[]}"));

        adapter.query(Map.of("spec.type", "image"), "Bearer abc").collectList().block();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer abc");
    }

    @Test
    void retriesOnServerErrorThenSucceeds() {
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"items\":[{\"metadata\":{\"uid\":\"1\"}}]}"));

        StepVerifier.create(adapter.query(Map.of("spec.type", "image"), null))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(server.getRequestCount()).isEqualTo(2);
    }
}
