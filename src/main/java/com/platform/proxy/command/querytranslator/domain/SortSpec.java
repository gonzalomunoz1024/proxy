package com.platform.proxy.command.querytranslator.domain;

import lombok.Builder;
import lombok.Value;

/**
 * Sort parameters passed through to downstream calls.
 */
@Value
@Builder
public class SortSpec {

    String sort;
    String order;

    public boolean isPresent() {
        return sort != null && !sort.isBlank();
    }
}
