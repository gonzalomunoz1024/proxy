package com.platform.proxy.core.utils;

import com.platform.proxy.command.querytranslator.domain.FilterCondition;
import com.platform.proxy.command.querytranslator.domain.FilterGroup;
import com.platform.proxy.command.querytranslator.domain.FilterOperator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes Backstage filter strings into structured {@link FilterGroup}s.
 * <p>
 * A single filter param value is an AND group whose conditions are
 * comma-separated, e.g. {@code spec.type=image,spec.appId=CLAUT}. Each
 * condition is split on its operator. {@code LIKE} ({@code field=~value}) and
 * {@code IN} ({@code field=in(a|b)}) are detected by dedicated value markers.
 */
@Component
public class FilterStringTokenizer {

    /** Parses one filter param value (one AND group). */
    public FilterGroup tokenizeGroup(String filter) {
        if (filter == null || filter.isBlank()) {
            throw new IllegalArgumentException("filter must not be blank");
        }
        List<FilterCondition> conditions = new ArrayList<>();
        for (String rawCondition : filter.split(",")) {
            String condition = rawCondition.trim();
            if (!condition.isEmpty()) {
                conditions.add(tokenizeCondition(condition));
            }
        }
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("filter contained no valid conditions: " + filter);
        }
        return FilterGroup.builder().conditions(conditions).build();
    }

    /** Parses a single {@code field<op>value} condition. */
    public FilterCondition tokenizeCondition(String condition) {
        int eqIndex = condition.indexOf('=');

        // Marker-based operators take precedence: field=~value, field=in(...)
        if (eqIndex >= 0) {
            String afterEq = condition.substring(eqIndex + 1);
            if (afterEq.startsWith("~")) {
                return build(condition.substring(0, eqIndex), FilterOperator.LIKE, afterEq.substring(1));
            }
            if (afterEq.startsWith("in(") && afterEq.endsWith(")")) {
                return build(condition.substring(0, eqIndex), FilterOperator.IN,
                        afterEq.substring(3, afterEq.length() - 1));
            }
        }

        for (FilterOperator op : FilterOperator.scanOrder()) {
            int idx = condition.indexOf(op.symbol());
            if (idx > 0) {
                String field = condition.substring(0, idx);
                String value = condition.substring(idx + op.symbol().length());
                return build(field, op, value);
            }
        }
        throw new IllegalArgumentException("malformed filter condition (no operator): " + condition);
    }

    private FilterCondition build(String field, FilterOperator op, String value) {
        String trimmedField = field.trim();
        String trimmedValue = value.trim();
        if (trimmedField.isEmpty()) {
            throw new IllegalArgumentException("filter condition has empty field");
        }
        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException("filter condition has empty value for field: " + trimmedField);
        }
        return FilterCondition.builder()
                .field(trimmedField)
                .operator(op)
                .value(trimmedValue)
                .build();
    }
}
