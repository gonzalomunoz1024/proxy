package com.platform.proxy.core.utils;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Encodes query parameter values for safe inclusion in downstream URLs,
 * guarding against query injection.
 */
@Component
public class QueryParamEncoder {

    public String encode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
