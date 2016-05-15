package com.ge.snowizard.client;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;
import io.dropwizard.setup.Environment;

public class SnowizardClientBuilder {
    private final Environment environment;

    /**
     * Constructor
     *
     * @param environment
     *            Environment
     */
    public SnowizardClientBuilder(@Nonnull final Environment environment) {
        this.environment = Objects.requireNonNull(environment);
    }

    /**
     * Build a new {@link SnowizardClient}
     * 
     * @param configuration
     *            Configuration to use for the client
     * @return new SnowizardClient
     */
    public SnowizardClient build(
            @Nonnull final SnowizardClientConfiguration configuration) {
        final Client client = new JerseyClientBuilder(environment)
                .using(configuration)
                .withProvider(ProtocolBufferMessageBodyProvider.class)
                .build("snowizard");
        return build(configuration, client);
    }

    /**
     * Build a new {@link SnowizardClient}. If using this method instead of
     * {@link #build(SnowizardClientConfiguration)}, remember to register the
     * {@link ProtocolBufferMessageBodyProvider}.
     * 
     * @param configuration
     *            Configuration to use for the client
     * @param client
     *            Jersey Client to use
     * @return new SnowizardClient
     */
    public SnowizardClient build(
            @Nonnull final SnowizardClientConfiguration configuration,
            @Nonnull final Client client) {
        return new SnowizardClient(environment.metrics(), client,
                configuration.getUri());
    }
}
