package com.platform.proxy.command.querytranslator.ports.inbound;

import com.platform.proxy.command.querytranslator.domain.command.TranslateQueryCommand;
import com.platform.proxy.command.querytranslator.domain.dto.outbound.TranslatedQueryResponseDTO;
import reactor.core.publisher.Mono;

/**
 * Inbound port: the entry point into the bounded context. Implemented by the
 * REST controller inbound adapter.
 */
public interface TranslateQueryPort {

    Mono<TranslatedQueryResponseDTO> translate(TranslateQueryCommand command, String authorization);
}
