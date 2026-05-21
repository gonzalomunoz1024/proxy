package com.platform.proxy.command.querytranslator.domain.event;

import com.platform.proxy.command.querytranslator.domain.QueryPlan;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Published via {@code ApplicationEventPublisher} after a successful
 * translation. Consumed by other bounded contexts (audit, analytics) using
 * {@code @EventListener}.
 */
@Data
@SuperBuilder
public class QueryTranslatedEvent {

    private final QueryPlan plan;
    private final int resultCount;
    private final long latencyMs;
    private final boolean degraded;
    private final Instant occurredAt;
}
