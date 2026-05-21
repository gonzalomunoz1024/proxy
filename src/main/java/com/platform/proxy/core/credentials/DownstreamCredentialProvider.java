package com.platform.proxy.core.credentials;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Supplies credentials for downstream calls. The inbound bearer token is
 * propagated through here so use cases never handle raw credentials.
 */
@Component
public class DownstreamCredentialProvider {

    /**
     * Resolves the Authorization header value to forward downstream.
     *
     * @param inboundAuthorization the inbound Authorization header (may be null)
     * @return the header value to forward, if any
     */
    public Optional<String> resolveAuthorization(String inboundAuthorization) {
        if (inboundAuthorization == null || inboundAuthorization.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(inboundAuthorization);
    }
}
