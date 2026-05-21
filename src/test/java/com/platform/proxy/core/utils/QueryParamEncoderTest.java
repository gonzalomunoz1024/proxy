package com.platform.proxy.core.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryParamEncoderTest {

    private final QueryParamEncoder encoder = new QueryParamEncoder();

    @Test
    void encodesSpecialCharacters() {
        assertThat(encoder.encode("a b&c")).isEqualTo("a+b%26c");
    }

    @Test
    void encodesNullAsEmpty() {
        assertThat(encoder.encode(null)).isEmpty();
    }

    @Test
    void leavesPlainValueIntact() {
        assertThat(encoder.encode("image")).isEqualTo("image");
    }
}
