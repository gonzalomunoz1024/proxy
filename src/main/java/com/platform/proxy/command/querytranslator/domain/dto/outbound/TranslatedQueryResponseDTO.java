package com.platform.proxy.command.querytranslator.domain.dto.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Aggregated, deduplicated, paginated response returned to the caller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranslatedQueryResponseDTO {

    private List<Map<String, Object>> items;
    private int total;
    /** True if one or more downstream OR-group calls failed (partial result). */
    private boolean degraded;
    private PageMetadata page;
}
