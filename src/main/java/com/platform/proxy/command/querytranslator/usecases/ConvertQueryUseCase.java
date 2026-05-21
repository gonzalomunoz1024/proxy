package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.FilterGroup;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Converts a parsed {@link FilterGroup} into a downstream query-param map.
 */
public interface ConvertQueryUseCase {

    Mono<Map<String, String>> execute(FilterGroup group);
}
