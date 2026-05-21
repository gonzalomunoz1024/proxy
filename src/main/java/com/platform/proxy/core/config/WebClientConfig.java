package com.platform.proxy.core.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.concurrent.TimeUnit;

/**
 * Configures the pooled, bounded {@link WebClient} used by outbound adapters.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient downstreamWebClient(
            @Value("${proxy.downstream.base-url}") String baseUrl,
            @Value("${proxy.downstream.timeout-ms:5000}") int timeoutMs) {

        ConnectionProvider provider = ConnectionProvider.builder("downstream")
                .maxConnections(200)
                .pendingAcquireMaxCount(1000)
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                // Use the JDK address resolver instead of Netty's native resolver,
                // which can fail to resolve hosts in some environments.
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
