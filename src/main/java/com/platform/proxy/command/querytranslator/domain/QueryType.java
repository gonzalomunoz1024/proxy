package com.platform.proxy.command.querytranslator.domain;

/**
 * Discriminator used to select a {@code TranslateQueryUseCase} from the
 * registry built dynamically in {@code MainConfig}.
 */
public enum QueryType {
    BACKSTAGE
}
