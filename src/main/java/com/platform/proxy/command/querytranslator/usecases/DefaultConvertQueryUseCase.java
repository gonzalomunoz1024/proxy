package com.platform.proxy.command.querytranslator.usecases;

import com.platform.proxy.command.querytranslator.domain.FilterGroup;
import com.platform.proxy.command.querytranslator.domain.converter.FilterGroupToQueryParamConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class DefaultConvertQueryUseCase implements ConvertQueryUseCase {

    private final FilterGroupToQueryParamConverter converter;

    public DefaultConvertQueryUseCase(FilterGroupToQueryParamConverter converter) {
        this.converter = converter;
    }

    @Override
    public Mono<Map<String, String>> execute(FilterGroup group) {
        return Mono.fromSupplier(() -> converter.convert(group));
    }
}
