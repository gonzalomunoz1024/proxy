package com.platform.proxy.command.querytranslator.ports.outbound;

import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Outbound port for querying a downstream REST provider. Implemented by an
 * outbound adapter; use cases depend only on this interface.
 */
public interface DownstreamQueryPort {

    /**
     * Executes a single downstream query.
     *
     * @param queryParams    conventional REST query params for one OR group
     * @param authorization  Authorization header to forward (may be null)
     * @return a flux of result items as generic maps
     */
    Flux<Map<String, Object>> query(Map<String, String> queryParams, String authorization);
}
