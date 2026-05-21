package com.platform.proxy.command.querytranslator.domain.command;

import com.platform.proxy.command.querytranslator.domain.PageSpec;
import com.platform.proxy.command.querytranslator.domain.SortSpec;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Command issued by the inbound adapter to translate a Backstage query.
 */
@Data
@Builder
public class TranslateQueryCommand {

    /** Raw Backstage filter param values (the OR set). */
    private final List<String> filters;
    private final PageSpec page;
    private final SortSpec sort;
}
