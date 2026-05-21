package com.platform.proxy.command.querytranslator.domain.dto.inbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Inbound request representation. {@code filter} carries the OR set of raw
 * Backstage filter param values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranslateRequestDTO {

    private List<String> filter;
    private Integer limit;
    private Integer offset;
    private String sort;
    private String order;
}
