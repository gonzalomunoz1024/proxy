package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.FilterGroup;
import com.platform.proxy.command.querytranslator.domain.PageSpec;
import com.platform.proxy.command.querytranslator.domain.QueryPlan;
import com.platform.proxy.command.querytranslator.domain.QueryType;
import com.platform.proxy.command.querytranslator.domain.SortSpec;
import com.platform.proxy.command.querytranslator.domain.command.TranslateQueryCommand;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.TranslatedQueryResponseDTO;
import com.platform.proxy.core.utils.FilterStringTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full flow: parse filters into a {@link QueryPlan}, fan out
 * downstream, then merge. Contains no external calls — all I/O is delegated to
 * outbound-port-backed use cases.
 */
@Service
public class DefaultTranslateQueryUseCase implements TranslateQueryUseCase {

    private final FilterStringTokenizer tokenizer;
    private final ExecuteDownstreamUseCase executeDownstreamUseCase;
    private final MergeResultsUseCase mergeResultsUseCase;
    private final int maxOrGroups;

    public DefaultTranslateQueryUseCase(FilterStringTokenizer tokenizer,
                                        ExecuteDownstreamUseCase executeDownstreamUseCase,
                                        MergeResultsUseCase mergeResultsUseCase,
                                        @Value("${proxy.downstream.max-or-groups:25}") int maxOrGroups) {
        this.tokenizer = tokenizer;
        this.executeDownstreamUseCase = executeDownstreamUseCase;
        this.mergeResultsUseCase = mergeResultsUseCase;
        this.maxOrGroups = maxOrGroups;
    }

    @Override
    public Mono<TranslatedQueryResponseDTO> execute(TranslateQueryCommand command, String authorization) {
        return Mono.fromSupplier(() -> parse(command))
                .flatMap(plan -> {
                    long start = System.nanoTime();
                    return mergeResultsUseCase.execute(
                            executeDownstreamUseCase.execute(plan, authorization), plan, start);
                });
    }

    private QueryPlan parse(TranslateQueryCommand command) {
        List<String> filters = command.getFilters();
        if (filters == null || filters.isEmpty()) {
            throw new IllegalArgumentException("at least one filter is required");
        }
        if (filters.size() > maxOrGroups) {
            throw new IllegalArgumentException(
                    "too many OR groups: " + filters.size() + " (max " + maxOrGroups + ")");
        }
        List<FilterGroup> orGroups = new ArrayList<>();
        for (String filter : filters) {
            orGroups.add(tokenizer.tokenizeGroup(filter));
        }
        return QueryPlan.builder()
                .orGroups(orGroups)
                .page(command.getPage() != null ? command.getPage() : PageSpec.builder().build())
                .sort(command.getSort() != null ? command.getSort() : SortSpec.builder().build())
                .build();
    }

    @Override
    public QueryType supportedType() {
        return QueryType.BACKSTAGE;
    }
}
