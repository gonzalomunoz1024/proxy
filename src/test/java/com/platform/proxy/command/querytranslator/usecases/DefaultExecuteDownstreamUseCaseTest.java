package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.FilterCondition;
import com.platform.proxy.command.querytranslator.domain.FilterGroup;
import com.platform.proxy.command.querytranslator.domain.FilterOperator;
import com.platform.proxy.command.querytranslator.domain.QueryPlan;
import com.platform.proxy.command.querytranslator.domain.converter.FilterGroupToQueryParamConverter;
import com.platform.proxy.testing.querytranslator.adapters.outbound.InMemoryDownstreamAdapter;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;

import static com.platform.proxy.testing.querytranslator.fixtures.FilterFixtures.item;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultExecuteDownstreamUseCaseTest {

    private final ConvertQueryUseCase convert =
            new DefaultConvertQueryUseCase(new FilterGroupToQueryParamConverter());

    @Test
    void executesOneCallPerOrGroup() {
        InMemoryDownstreamAdapter adapter = new InMemoryDownstreamAdapter()
                .withResponder(params -> Flux.just(item("1", "a")));
        DefaultExecuteDownstreamUseCase useCase = new DefaultExecuteDownstreamUseCase(convert, adapter);

        QueryPlan plan = QueryPlan.builder()
                .orGroup(group("spec.type", "image"))
                .orGroup(group("spec.available", "true"))
                .build();

        List<Tuple2<List<Map<String, Object>>, Boolean>> results =
                useCase.execute(plan, null).collectList().block();

        assertThat(adapter.callCount()).isEqualTo(2);
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(Tuple2::getT2);
    }

    @Test
    void degradesGroupOnFailureWithoutFailingWholeRequest() {
        InMemoryDownstreamAdapter adapter = new InMemoryDownstreamAdapter()
                .withResponder(params -> params.containsKey("spec.broken")
                        ? Flux.error(new RuntimeException("downstream 500"))
                        : Flux.just(item("1", "a")));
        DefaultExecuteDownstreamUseCase useCase = new DefaultExecuteDownstreamUseCase(convert, adapter);

        QueryPlan plan = QueryPlan.builder()
                .orGroup(group("spec.type", "image"))
                .orGroup(group("spec.broken", "x"))
                .build();

        List<Tuple2<List<Map<String, Object>>, Boolean>> results =
                useCase.execute(plan, null).collectList().block();

        assertThat(results).hasSize(2);
        assertThat(results).anyMatch(t -> !t.getT2());
        assertThat(results).anyMatch(Tuple2::getT2);
    }

    private FilterGroup group(String field, String value) {
        return FilterGroup.builder()
                .condition(FilterCondition.builder()
                        .field(field).operator(FilterOperator.EQ).value(value).build())
                .build();
    }
}
