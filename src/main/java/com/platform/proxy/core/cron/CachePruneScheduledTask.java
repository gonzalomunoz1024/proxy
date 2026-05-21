package com.platform.proxy.core.cron;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically performs cache maintenance (Phase 12). Caffeine evicts lazily;
 * this forces cleanup and logs cache statistics for observability.
 */
@Component
public class CachePruneScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(CachePruneScheduledTask.class);

    private final Cache<String, Object> queryCache;

    public CachePruneScheduledTask(Cache<String, Object> queryCache) {
        this.queryCache = queryCache;
    }

    @Scheduled(fixedDelayString = "${proxy.cache.prune-interval-ms:60000}")
    public void prune() {
        queryCache.cleanUp();
        log.debug("Query cache pruned. size={}, stats={}",
                queryCache.estimatedSize(), queryCache.stats());
    }
}
