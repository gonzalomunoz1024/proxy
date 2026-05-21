package com.platform.proxy.command.querytranslator.domain;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * An AND group: all conditions must hold. Multiple {@link FilterGroup}s in a
 * {@link QueryPlan} form the OR set (their results are unioned).
 */
@Value
@Builder
public class FilterGroup {

    @Singular
    List<FilterCondition> conditions;
}
