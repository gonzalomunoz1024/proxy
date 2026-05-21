package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.QueryType;
import com.platform.proxy.command.querytranslator.domain.command.TranslateQueryCommand;
import com.platform.proxy.command.querytranslator.domain.converter.FilterGroupToQueryParamConverter;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.TranslatedQueryResponseDTO;
import com.platform.proxy.core.utils.FilterStringTokenizer;
import com.platform.proxy.testing.querytranslator.adapters.outbound.InMemoryDownstreamAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static com.platform.proxy.testing.querytranslator.fixtures.FilterFixtures.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultTranslateQueryUseCaseTest {

    private final InMemoryDownstreamAdapter adapter = new InMemoryDownstreamAdapter();
    private final DefaultTranslateQueryUseCase useCase = build();

    private DefaultTranslateQueryUseCase build() {
        ConvertQueryUseCase convert =
                new DefaultConvertQueryUseCase(new FilterGroupToQueryParamConverter());
        ExecuteDownstreamUseCase execute = new DefaultExecuteDownstreamUseCase(convert, adapter);
        MergeResultsUseCase merge =
                new DefaultMergeResultsUseCase(mock(ApplicationEventPublisher.class), "metadata.uid");
        return new DefaultTranslateQueryUseCase(new FilterStringTokenizer(), execute, merge, 25);
    }

    @Test
    void translatesParsesFansOutAndMergesOrGroups() {
        adapter.withResponder(params -> {
            if ("image".equals(params.get("spec.type"))) {
                return Flux.just(item("1", "a"), item("2", "b"));
            }
            return Flux.just(item("2", "b"), item("3", "c"));
        });

        TranslateQueryCommand command = TranslateQueryCommand.builder()
                .filters(List.of("spec.type=image", "spec.available=true"))
                .build();

        StepVerifier.create(useCase.execute(command, null))
                .assertNext(resp -> {
                    assertThat(resp.getTotal()).isEqualTo(3); // deduped union
                    assertThat(adapter.callCount()).isEqualTo(2); // one per OR group
                })
                .verifyComplete();
    }

    @Test
    void rejectsEmptyFilters() {
        TranslateQueryCommand command = TranslateQueryCommand.builder().filters(List.of()).build();

        StepVerifier.create(useCase.execute(command, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void reportsSupportedType() {
        assertThat(useCase.supportedType()).isEqualTo(QueryType.BACKSTAGE);
    }

    @Test
    void emptyResponseWhenNoMatches() {
        adapter.withResponder(params -> Flux.empty());
        TranslateQueryCommand command = TranslateQueryCommand.builder()
                .filters(List.of("spec.type=none")).build();

        TranslatedQueryResponseDTO resp = useCase.execute(command, null).block();
        assertThat(resp).isNotNull();
        assertThat(resp.getItems()).isEmpty();
    }
}
