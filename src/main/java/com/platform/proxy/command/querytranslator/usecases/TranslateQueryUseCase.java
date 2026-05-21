package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.QueryType;
import com.platform.proxy.command.querytranslator.domain.command.TranslateQueryCommand;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.TranslatedQueryResponseDTO;
import reactor.core.publisher.Mono;

/**
 * Application service orchestrating the full translation flow:
 * parse → convert → execute downstream → merge.
 */
public interface TranslateQueryUseCase {

    Mono<TranslatedQueryResponseDTO> execute(TranslateQueryCommand command, String authorization);

    /** Query dialect this use case handles; used to build the registry. */
    QueryType supportedType();
}
