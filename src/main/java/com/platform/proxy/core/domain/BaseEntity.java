package com.platform.proxy.core.domain;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Root of domain inheritance hierarchies. Domain models that participate in
 * inheritance use {@link SuperBuilder} and extend this type.
 */
@Data
@SuperBuilder
public abstract class BaseEntity {

    private final Instant createdAt;
}
