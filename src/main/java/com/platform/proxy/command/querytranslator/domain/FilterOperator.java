package com.platform.proxy.command.querytranslator.domain;

import java.util.List;

/**
 * Comparison operators supported in Backstage-style filter conditions.
 * Two-character symbols must be ordered before their single-character
 * prefixes so the tokenizer matches the longest operator first.
 */
public enum FilterOperator {

    GTE(">="),
    LTE("<="),
    NEQ("!="),
    EQ("="),
    GT(">"),
    LT("<"),
    /** {@code field=~value} — substring match. */
    LIKE("=~"),
    /** {@code field=in(a|b|c)} — membership. */
    IN("=in");

    private final String symbol;

    FilterOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    /**
     * Operators ordered for longest-match tokenizing. {@link #LIKE} and
     * {@link #IN} are detected by dedicated value markers, not by scanning,
     * so they are excluded here.
     */
    public static List<FilterOperator> scanOrder() {
        return List.of(GTE, LTE, NEQ, EQ, GT, LT);
    }
}
