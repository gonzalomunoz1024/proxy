package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.PageSpec;
import com.platform.proxy.command.querytranslator.domain.QueryPlan;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.TranslatedQueryResponseDTO;
import com.platform.proxy.command.querytranslator.domain.event.QueryTranslatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.platform.proxy.testing.querytranslator.fixtures.FilterFixtures.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class DefaultMergeResultsUseCaseTest {

    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final DefaultMergeResultsUseCase useCase =
            new DefaultMergeResultsUseCase(publisher, "metadata.uid");

    @Test
    void deduplicatesByIdAcrossOrGroups() {
        Flux<Tuple2<List<Map<String, Object>>, Boolean>> groups = Flux.just(
                Tuples.of(List.of(item("1", "a"), item("2", "b")), true),
                Tuples.of(List.of(item("2", "b"), item("3", "c")), true));

        StepVerifier.create(useCase.execute(groups, plan(null), System.nanoTime()))
                .assertNext(resp -> {
                    assertThat(resp.getTotal()).isEqualTo(3);
                    assertThat(resp.isDegraded()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void flagsDegradedWhenAGroupFailed() {
        Flux<Tuple2<List<Map<String, Object>>, Boolean>> groups = Flux.just(
                Tuples.of(List.of(item("1", "a")), true),
                Tuples.of(List.of(), false));

        TranslatedQueryResponseDTO resp =
                useCase.execute(groups, plan(null), System.nanoTime()).block();

        assertThat(resp).isNotNull();
        assertThat(resp.isDegraded()).isTrue();
        assertThat(resp.getTotal()).isEqualTo(1);
    }

    @Test
    void appliesPaginationAfterMerge() {
        Flux<Tuple2<List<Map<String, Object>>, Boolean>> groups = Flux.just(
                Tuples.of(List.of(item("1", "a"), item("2", "b"), item("3", "c"), item("4", "d")), true));

        PageSpec page = PageSpec.builder().limit(2).offset(1).build();

        TranslatedQueryResponseDTO resp = useCase.execute(groups, plan(page), System.nanoTime()).block();

        assertThat(resp).isNotNull();
        assertThat(resp.getItems()).hasSize(2);
        assertThat(resp.getItems().get(0)).isEqualTo(item("2", "b"));
    }

    @Test
    void publishesQueryTranslatedEvent() {
        AtomicReference<Object> captured = new AtomicReference<>();
        doAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return null;
        }).when(publisher).publishEvent(any(Object.class));

        Flux<Tuple2<List<Map<String, Object>>, Boolean>> groups =
                Flux.just(Tuples.of(List.of(item("1", "a")), true));

        useCase.execute(groups, plan(null), System.nanoTime()).block();

        assertThat(captured.get()).isInstanceOf(QueryTranslatedEvent.class);
        assertThat(((QueryTranslatedEvent) captured.get()).getResultCount()).isEqualTo(1);
    }

    @Test
    void keepsItemsWithoutIdAndDoesNotDeduplicateThem() {
        Map<String, Object> noId = Map.of("spec", Map.of("type", "image"));
        Map<String, Object> nonMapMeta = Map.of("metadata", "not-a-map");
        Flux<Tuple2<List<Map<String, Object>>, Boolean>> groups = Flux.just(
                Tuples.of(List.of(item("1", "a"), noId, nonMapMeta), true));

        TranslatedQueryResponseDTO resp = useCase.execute(groups, plan(null), System.nanoTime()).block();

        assertThat(resp).isNotNull();
        assertThat(resp.getTotal()).isEqualTo(3); // one with id + two without
    }

    private QueryPlan plan(PageSpec page) {
        return QueryPlan.builder().page(page).build();
    }
}
