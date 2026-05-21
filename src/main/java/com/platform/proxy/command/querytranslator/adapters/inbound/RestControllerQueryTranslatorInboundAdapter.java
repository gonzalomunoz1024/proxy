package com.platform.proxy.command.querytranslator.adapters.inbound;

import com.platform.proxy.command.querytranslator.domain.PageSpec;
import com.platform.proxy.command.querytranslator.domain.QueryType;
import com.platform.proxy.command.querytranslator.domain.SortSpec;
import com.platform.proxy.command.querytranslator.domain.command.TranslateQueryCommand;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.ErrorResponseView;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.TranslatedQueryResponseDTO;
import com.platform.proxy.command.querytranslator.ports.inbound.TranslateQueryPort;
import com.platform.proxy.command.querytranslator.usecases.TranslateQueryUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Inbound adapter exposing the Backstage-style query endpoint and implementing
 * {@link TranslateQueryPort}. Selects the use case from the dynamically-built
 * registry keyed by {@link QueryType}.
 */
@RestController
public class RestControllerQueryTranslatorInboundAdapter implements TranslateQueryPort {

    private final Map<QueryType, TranslateQueryUseCase> useCaseRegistry;

    public RestControllerQueryTranslatorInboundAdapter(
            Map<QueryType, TranslateQueryUseCase> translateUseCaseRegistry) {
        this.useCaseRegistry = translateUseCaseRegistry;
    }

    @GetMapping(value = "/translator/entities", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<TranslatedQueryResponseDTO> getEntities(
            @RequestParam("filter") List<String> filter,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "order", required = false) String order,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        TranslateQueryCommand command = TranslateQueryCommand.builder()
                .filters(filter)
                .page(PageSpec.builder().limit(limit).offset(offset).build())
                .sort(SortSpec.builder().sort(sort).order(order).build())
                .build();

        return translate(command, authorization);
    }

    @Override
    public Mono<TranslatedQueryResponseDTO> translate(TranslateQueryCommand command, String authorization) {
        TranslateQueryUseCase useCase = useCaseRegistry.get(QueryType.BACKSTAGE);
        if (useCase == null) {
            return Mono.error(new IllegalStateException("no use case registered for BACKSTAGE"));
        }
        return useCase.execute(command, authorization);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseView> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseView.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Bad Request")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }
}
