package com.platform.proxy.command.querytranslator.domain.converter;

import com.platform.proxy.command.querytranslator.domain.FilterCondition;
import com.platform.proxy.command.querytranslator.domain.FilterGroup;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts a single {@link FilterGroup} (AND group) into a conventional REST
 * query-param map. Nested field names are preserved as map keys; operator
 * semantics other than equality are encoded into the value via an operator
 * prefix that the downstream understands.
 */
@Component
public class FilterGroupToQueryParamConverter {

    public Map<String, String> convert(FilterGroup group) {
        Map<String, String> params = new LinkedHashMap<>();
        for (FilterCondition condition : group.getConditions()) {
            params.put(condition.getField(), encodeValue(condition));
        }
        return params;
    }

    private String encodeValue(FilterCondition condition) {
        return switch (condition.getOperator()) {
            case EQ -> condition.getValue();
            case NEQ -> "ne:" + condition.getValue();
            case GT -> "gt:" + condition.getValue();
            case LT -> "lt:" + condition.getValue();
            case GTE -> "gte:" + condition.getValue();
            case LTE -> "lte:" + condition.getValue();
            case LIKE -> "like:" + condition.getValue();
            case IN -> "in:" + condition.getValue();
        };
    }
}
