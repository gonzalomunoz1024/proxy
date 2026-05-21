package com.platform.proxy.command.querytranslator.domain;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * The fully-parsed query: an OR set of AND groups plus pagination and sorting.
 */
@Value
@Builder
public class QueryPlan {

    @Singular
    List<FilterGroup> orGroups;
    PageSpec page;
    SortSpec sort;
}
