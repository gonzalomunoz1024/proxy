package com.platform.proxy.command.querytranslator.domain.converter;

import com.platform.proxy.command.querytranslator.domain.FilterCondition;
import com.platform.proxy.command.querytranslator.domain.FilterGroup;
import com.platform.proxy.command.querytranslator.domain.FilterOperator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilterGroupToQueryParamConverterTest {

    private final FilterGroupToQueryParamConverter converter = new FilterGroupToQueryParamConverter();

    @Test
    void mapsEqualityPreservingNestedKeys() {
        FilterGroup group = FilterGroup.builder()
                .condition(cond("spec.type", FilterOperator.EQ, "image"))
                .condition(cond("spec.appId", FilterOperator.EQ, "CLAUT"))
                .build();

        Map<String, String> params = converter.convert(group);

        assertThat(params).containsEntry("spec.type", "image");
        assertThat(params).containsEntry("spec.appId", "CLAUT");
    }

    @Test
    void encodesEveryNonEqualityOperatorWithPrefix() {
        FilterGroup group = FilterGroup.builder()
                .condition(cond("a", FilterOperator.GTE, "1"))
                .condition(cond("b", FilterOperator.LTE, "2"))
                .condition(cond("c", FilterOperator.GT, "3"))
                .condition(cond("d", FilterOperator.LT, "4"))
                .condition(cond("e", FilterOperator.NEQ, "5"))
                .condition(cond("f", FilterOperator.LIKE, "web"))
                .condition(cond("g", FilterOperator.IN, "x|y"))
                .build();

        Map<String, String> params = converter.convert(group);

        assertThat(params).containsEntry("a", "gte:1");
        assertThat(params).containsEntry("b", "lte:2");
        assertThat(params).containsEntry("c", "gt:3");
        assertThat(params).containsEntry("d", "lt:4");
        assertThat(params).containsEntry("e", "ne:5");
        assertThat(params).containsEntry("f", "like:web");
        assertThat(params).containsEntry("g", "in:x|y");
    }

    private FilterCondition cond(String field, FilterOperator op, String value) {
        return FilterCondition.builder().field(field).operator(op).value(value).build();
    }
}
