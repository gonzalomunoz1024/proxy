package com.platform.proxy.command.querytranslator.domain.dto.outbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Loosely-typed envelope for a downstream response. Downstream payloads vary,
 * so items are kept as generic maps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DownstreamResourceDTO {

    private List<Map<String, Object>> items;
}
