package com.platform.proxy.command.querytranslator.domain;

import lombok.Builder;
import lombok.Value;

/**
 * Pagination parameters passed through to downstream calls and re-applied
 * after the cross-group merge.
 */
@Value
@Builder
public class PageSpec {

    Integer limit;
    Integer offset;

    public boolean hasLimit() {
        return limit != null;
    }

    public boolean hasOffset() {
        return offset != null && offset > 0;
    }
}
