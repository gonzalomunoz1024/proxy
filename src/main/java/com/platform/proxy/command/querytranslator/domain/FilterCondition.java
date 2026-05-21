package com.platform.proxy.command.querytranslator.domain;

import lombok.Builder;
import lombok.Value;

/**
 * A single {@code field <operator> value} predicate, e.g. {@code spec.type=image}.
 * Nested field names (dotted paths) are preserved verbatim.
 */
@Value
@Builder
public class FilterCondition {

    String field;
    FilterOperator operator;
    String value;
}
