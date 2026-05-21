package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.QueryPlan;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.TranslatedQueryResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;

/**
 * Merges per-group downstream results into a single deduplicated, paginated
 * response and publishes {@code QueryTranslatedEvent}.
 */
public interface MergeResultsUseCase {

    Mono<TranslatedQueryResponseDTO> execute(
            Flux<Tuple2<List<Map<String, Object>>, Boolean>> groupResults,
            QueryPlan plan,
            long startNanos);
}
