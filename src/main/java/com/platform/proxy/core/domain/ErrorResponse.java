package com.platform.proxy.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;
}
