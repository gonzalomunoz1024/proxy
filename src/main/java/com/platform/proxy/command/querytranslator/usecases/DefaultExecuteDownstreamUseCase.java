package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.PageSpec;
import com.platform.proxy.command.querytranslator.domain.QueryPlan;
import com.platform.proxy.command.querytranslator.domain.SortSpec;
import com.platform.proxy.command.querytranslator.ports.outbound.DownstreamQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;

/**
 * For each OR group: convert to query params, append pagination/sort
 * passthrough, call the downstream port, and collect results. A failing group
 * degrades to an empty list flagged {@code false} rather than failing the
 * whole request.
 */
@Service
public class DefaultExecuteDownstreamUseCase implements ExecuteDownstreamUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultExecuteDownstreamUseCase.class);

    private final ConvertQueryUseCase convertQueryUseCase;
    private final DownstreamQueryPort downstreamQueryPort;

    public DefaultExecuteDownstreamUseCase(ConvertQueryUseCase convertQueryUseCase,
                                           DownstreamQueryPort downstreamQueryPort) {
        this.convertQueryUseCase = convertQueryUseCase;
        this.downstreamQueryPort = downstreamQueryPort;
    }

    @Override
    public Flux<Tuple2<List<Map<String, Object>>, Boolean>> execute(QueryPlan plan, String authorization) {
        return Flux.fromIterable(plan.getOrGroups())
                .flatMap(group -> convertQueryUseCase.execute(group)
                        .map(params -> applyPassthrough(params, plan))
                        .flatMapMany(params -> downstreamQueryPort.query(params, authorization))
                        .collectList()
                        .map(items -> Tuples.of(items, true))
                        .onErrorResume(error -> {
                            log.warn("Downstream OR-group failed, degrading result: {}", error.toString());
                            return reactor.core.publisher.Mono.just(Tuples.of(List.<Map<String, Object>>of(), false));
                        }));
    }

    private Map<String, String> applyPassthrough(Map<String, String> params, QueryPlan plan) {
        PageSpec page = plan.getPage();
        if (page != null) {
            if (page.hasLimit()) {
                params.put("limit", String.valueOf(page.getLimit()));
            }
            if (page.hasOffset()) {
                params.put("offset", String.valueOf(page.getOffset()));
            }
        }
        SortSpec sort = plan.getSort();
        if (sort != null && sort.isPresent()) {
            params.put("sort", sort.getSort());
            if (sort.getOrder() != null && !sort.getOrder().isBlank()) {
                params.put("order", sort.getOrder());
            }
        }
        return params;
    }
}
