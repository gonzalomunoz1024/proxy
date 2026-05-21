package com.platform.proxy.testing.querytranslator.adapters.outbound;

import com.platform.proxy.command.querytranslator.ports.outbound.DownstreamQueryPort;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Test adapter implementing {@link DownstreamQueryPort} in memory, mirroring the
 * command structure under the {@code testing/} package. Records the query
 * params it received and returns canned results (or fails) per a configurable
 * response function.
 */
public class InMemoryDownstreamAdapter implements DownstreamQueryPort {

    private final List<Map<String, String>> receivedParams = new ArrayList<>();
    private Function<Map<String, String>, Flux<Map<String, Object>>> responder;

    public InMemoryDownstreamAdapter() {
        this.responder = params -> Flux.empty();
    }

    public InMemoryDownstreamAdapter withResponder(
            Function<Map<String, String>, Flux<Map<String, Object>>> responder) {
        this.responder = responder;
        return this;
    }

    @Override
    public Flux<Map<String, Object>> query(Map<String, String> queryParams, String authorization) {
        receivedParams.add(Map.copyOf(queryParams));
        return responder.apply(queryParams);
    }

    public List<Map<String, String>> receivedParams() {
        return receivedParams;
    }

    public int callCount() {
        return receivedParams.size();
    }
}
