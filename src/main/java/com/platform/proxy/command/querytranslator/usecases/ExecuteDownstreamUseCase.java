package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.QueryPlan;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;

/**
 * Executes downstream calls for every OR group of a {@link QueryPlan} and
 * surfaces per-group success/failure so partial failures can be tolerated.
 */
public interface ExecuteDownstreamUseCase {

    /**
     * @return a flux of tuples: (list of items for the group, group succeeded).
     *         A failed group emits an empty list with {@code false}.
     */
    Flux<Tuple2<List<Map<String, Object>>, Boolean>> execute(QueryPlan plan, String authorization);
}
