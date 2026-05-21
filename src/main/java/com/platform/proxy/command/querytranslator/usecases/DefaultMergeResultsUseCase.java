package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.PageSpec;
import com.platform.proxy.command.querytranslator.domain.QueryPlan;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.PageMetadata;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.TranslatedQueryResponseDTO;
import com.platform.proxy.command.querytranslator.domain.event.QueryTranslatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges per-group results into one deduplicated list (keyed by the configured
 * id field), re-applies pagination across the merged set, and publishes
 * {@link QueryTranslatedEvent}.
 */
@Service
public class DefaultMergeResultsUseCase implements MergeResultsUseCase {

    private final ApplicationEventPublisher eventPublisher;
    private final String idField;

    public DefaultMergeResultsUseCase(ApplicationEventPublisher eventPublisher,
                                      @Value("${proxy.downstream.id-field:metadata.uid}") String idField) {
        this.eventPublisher = eventPublisher;
        this.idField = idField;
    }

    @Override
    public Mono<TranslatedQueryResponseDTO> execute(
            Flux<Tuple2<List<Map<String, Object>>, Boolean>> groupResults,
            QueryPlan plan,
            long startNanos) {

        return groupResults.collectList().map(groups -> {
            boolean degraded = groups.stream().anyMatch(g -> !g.getT2());

            Map<Object, Map<String, Object>> dedup = new LinkedHashMap<>();
            List<Map<String, Object>> noId = new ArrayList<>();
            for (Tuple2<List<Map<String, Object>>, Boolean> group : groups) {
                for (Map<String, Object> item : group.getT1()) {
                    Object id = extractId(item);
                    if (id == null) {
                        noId.add(item);
                    } else {
                        dedup.putIfAbsent(id, item);
                    }
                }
            }

            List<Map<String, Object>> merged = new ArrayList<>(dedup.values());
            merged.addAll(noId);

            List<Map<String, Object>> paged = applyPagination(merged, plan.getPage());

            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            eventPublisher.publishEvent(QueryTranslatedEvent.builder()
                    .plan(plan)
                    .resultCount(paged.size())
                    .latencyMs(latencyMs)
                    .degraded(degraded)
                    .occurredAt(Instant.now())
                    .build());

            return TranslatedQueryResponseDTO.builder()
                    .items(paged)
                    .total(paged.size())
                    .degraded(degraded)
                    .page(toPageMetadata(plan.getPage()))
                    .build();
        });
    }

    private List<Map<String, Object>> applyPagination(List<Map<String, Object>> items, PageSpec page) {
        if (page == null) {
            return items;
        }
        int from = page.hasOffset() ? Math.min(page.getOffset(), items.size()) : 0;
        int to = page.hasLimit() ? Math.min(from + page.getLimit(), items.size()) : items.size();
        if (from == 0 && to == items.size()) {
            return items;
        }
        return new ArrayList<>(items.subList(from, to));
    }

    private PageMetadata toPageMetadata(PageSpec page) {
        if (page == null) {
            return PageMetadata.builder().build();
        }
        return PageMetadata.builder().limit(page.getLimit()).offset(page.getOffset()).build();
    }

    /** Reads a (possibly dotted) id path out of a nested result map. */
    @SuppressWarnings("unchecked")
    private Object extractId(Map<String, Object> item) {
        String[] parts = idField.split("\\.");
        Object current = item;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }
}
