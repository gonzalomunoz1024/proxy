package com.platform.proxy.testing.querytranslator.fixtures;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reusable test fixtures for downstream result items.
 */
public final class FilterFixtures {

    private FilterFixtures() {
    }

    /** A downstream item with a nested {@code metadata.uid} id. */
    public static Map<String, Object> item(String uid, String name) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("uid", uid);
        metadata.put("name", name);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("metadata", metadata);
        return item;
    }
}
