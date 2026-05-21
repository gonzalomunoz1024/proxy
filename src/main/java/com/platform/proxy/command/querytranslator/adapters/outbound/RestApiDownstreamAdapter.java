package com.platform.proxy.command.querytranslator.adapters.outbound;

import com.platform.proxy.command.querytranslator.domain.dto.outbound.DownstreamResourceDTO;
import com.platform.proxy.command.querytranslator.ports.outbound.DownstreamQueryPort;
import com.platform.proxy.core.credentials.DownstreamCredentialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Outbound adapter implementing {@link DownstreamQueryPort} via a reactive
 * {@link WebClient}. Applies per-call timeout and exponential-backoff retry.
 */
@Component
public class RestApiDownstreamAdapter implements DownstreamQueryPort {

    private static final Logger log = LoggerFactory.getLogger(RestApiDownstreamAdapter.class);

    private final WebClient webClient;
    private final DownstreamCredentialProvider credentialProvider;
    private final String path;
    private final Duration timeout;
    private final int maxAttempts;
    private final Duration backoff;

    public RestApiDownstreamAdapter(
            WebClient downstreamWebClient,
            DownstreamCredentialProvider credentialProvider,
            @Value("${proxy.downstream.path:/resources}") String path,
            @Value("${proxy.downstream.timeout-ms:5000}") long timeoutMs,
            @Value("${proxy.downstream.retry.max-attempts:3}") int maxAttempts,
            @Value("${proxy.downstream.retry.backoff-ms:200}") long backoffMs) {
        this.webClient = downstreamWebClient;
        this.credentialProvider = credentialProvider;
        this.path = path;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.maxAttempts = maxAttempts;
        this.backoff = Duration.ofMillis(backoffMs);
    }

    @Override
    public Flux<Map<String, Object>> query(Map<String, String> queryParams, String authorization) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath(path);
        queryParams.forEach(uri::queryParam);
        String relativeUri = uri.build().toUriString();

        WebClient.RequestHeadersSpec<?> request = webClient.get().uri(relativeUri);
        credentialProvider.resolveAuthorization(authorization)
                .ifPresent(token -> request.header(HttpHeaders.AUTHORIZATION, token));

        return request.retrieve()
                .bodyToMono(DownstreamResourceDTO.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(Math.max(0, maxAttempts - 1), backoff)
                        .filter(this::isRetryable)
                        .doBeforeRetry(sig -> log.debug("Retrying downstream call, attempt {}",
                                sig.totalRetries() + 1)))
                .flatMapMany(dto -> Flux.fromIterable(
                        dto.getItems() == null ? List.<Map<String, Object>>of() : dto.getItems()));
    }

    private boolean isRetryable(Throwable throwable) {
        // Retry transient failures; do not retry 4xx client errors.
        if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException ex) {
            return ex.getStatusCode().is5xxServerError();
        }
        return true;
    }
}
