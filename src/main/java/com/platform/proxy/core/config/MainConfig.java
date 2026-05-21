package com.platform.proxy.core.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.platform.proxy.command.querytranslator.domain.QueryType;
import com.platform.proxy.command.querytranslator.usecases.TranslateQueryUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central bean wiring. Builds registries dynamically from injected beans so new
 * use cases register themselves simply by being Spring components.
 */
@Configuration
public class MainConfig {

    /** Registry of translate use cases keyed by the query dialect they handle. */
    @Bean
    public Map<QueryType, TranslateQueryUseCase> translateUseCaseRegistry(
            List<TranslateQueryUseCase> useCases) {
        return useCases.stream()
                .collect(Collectors.toUnmodifiableMap(
                        TranslateQueryUseCase::supportedType, Function.identity()));
    }

    /** Query-plan / response cache (Phase 12). */
    @Bean
    public Cache<String, Object> queryCache(
            @Value("${proxy.cache.ttl-seconds:60}") long ttlSeconds,
            @Value("${proxy.cache.max-size:10000}") long maxSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }
}
