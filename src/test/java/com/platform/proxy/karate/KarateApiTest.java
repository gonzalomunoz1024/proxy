package com.platform.proxy.karate;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the application on a random port with a stubbed downstream, then runs
 * the Karate feature suite against it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KarateApiTest {

    private static MockWebServer downstream;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startDownstream() throws IOException {
        downstream = new MockWebServer();
        downstream.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                if (path.startsWith("/resources")) {
                    return new MockResponse()
                            .setHeader("Content-Type", "application/json")
                            .setBody("{\"items\":[{\"metadata\":{\"uid\":\"1\"}},"
                                    + "{\"metadata\":{\"uid\":\"2\"}}]}");
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        downstream.start();
    }

    @AfterAll
    static void stopDownstream() throws IOException {
        downstream.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("proxy.downstream.base-url", () -> "http://127.0.0.1:" + downstream.getPort() + "/");
        registry.add("proxy.downstream.id-field", () -> "metadata.uid");
        registry.add("proxy.downstream.timeout-ms", () -> "15000");
    }

    @Test
    void runsKarateSuite() {
        Results results = Runner.path("classpath:karate/translator.feature")
                .systemProperty("proxy.port", String.valueOf(port))
                .outputCucumberJson(true)
                .parallel(1);
        assertThat(results.getFailCount())
                .as("Karate scenarios failed:\n%s", results.getErrorMessages())
                .isEqualTo(0);
    }
}
