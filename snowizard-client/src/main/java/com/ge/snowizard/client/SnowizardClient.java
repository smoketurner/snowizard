package com.ge.snowizard.client;

import static com.codahale.metrics.MetricRegistry.name;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.ge.snowizard.api.protos.SnowizardProtos.SnowizardResponse;
import com.ge.snowizard.client.exceptions.SnowizardClientException;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;

public class SnowizardClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SnowizardClient.class);
    private static final String PING_RESPONSE = "pong";
    private final Timer fetchTimer;
    private final Client client;
    private final URI rootUri;

    /**
     * @deprecated Please use the {@link SnowizardClientBuilder} instead.
     */
    @Deprecated
    public SnowizardClient(@Nonnull final URI uri) {
        this(new MetricRegistry(), ClientBuilder.newClient(
                new ClientConfig(ProtocolBufferMessageBodyProvider.class)),
                uri);
    }

    /**
     * @deprecated Please use the {@link SnowizardClientBuilder} instead.
     */
    @Deprecated
    public SnowizardClient(@Nonnull final Client client,
            @Nonnull final URI uri) {
        this(new MetricRegistry(), client, uri);
    }

    /**
     * Constructor
     * 
     * @param registry
     *            MetricRegistry
     * @param client
     *            Jersey client
     * @param uri
     *            API endpoint
     */
    public SnowizardClient(@Nonnull final MetricRegistry registry,
            @Nonnull final Client client, @Nonnull final URI uri) {
        this.client = Objects.requireNonNull(client);
        this.rootUri = Objects.requireNonNull(uri);
        this.fetchTimer = registry.timer(name(SnowizardClient.class, "fetch"));
    }

    /**
     * Execute a request to the Snowizard service URL
     * 
     * @param count
     *            Number of IDs to generate
     * @return SnowizardResponse
     * @throws IOException
     *             Error in communicating with Snowizard
     */
    private SnowizardResponse executeRequest(final int count) {
        final URI uri = UriBuilder.fromUri(rootUri).path("/")
                .queryParam("count", count).build();
        LOGGER.debug("GET {}", uri);
        try (Timer.Context context = fetchTimer.time()) {
            return client.target(uri)
                    .request(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
                    .header(HttpHeaders.USER_AGENT, getUserAgent())
                    .get(SnowizardResponse.class);
        }
    }

    /**
     * Get a new ID from Snowizard
     * 
     * @return generated ID
     * @throws SnowizardClientException
     *             when unable to get an ID from any host
     */
    public long getId() throws SnowizardClientException {
        try {
            final SnowizardResponse snowizard = executeRequest(1);
            return snowizard.getId(0);
        } catch (final Exception e) {
            LOGGER.warn("Unable to get ID from host ({})", rootUri);
            throw new SnowizardClientException(
                    "Unable to generate ID from Snowizard", e);
        }
    }

    /**
     * Get multiple IDs from Snowizard
     * 
     * @param count
     *            Number of IDs to return
     * @return generated IDs
     * @throws SnowizardClientException
     *             when unable to get an ID from any host
     */
    public List<Long> getIds(final int count) throws SnowizardClientException {
        try {
            final SnowizardResponse snowizard = executeRequest(count);
            return snowizard.getIdList();
        } catch (final Exception e) {
            LOGGER.warn("Unable to get ID from host ({})", rootUri);
            throw new SnowizardClientException(
                    "Unable to generate batch of IDs from Snowizard", e);
        }
    }

    /**
     * Return the ping response
     *
     * @return true if the ping response was successful, otherwise false
     */
    public boolean ping() {
        final URI uri = UriBuilder.fromUri(rootUri).path("/ping").build();
        LOGGER.debug("GET {}", uri);
        final String response = client.target(uri).request().get(String.class);
        return PING_RESPONSE.equals(response);
    }

    /**
     * Return the service version
     *
     * @return service version
     */
    public String version() {
        final URI uri = UriBuilder.fromUri(rootUri).path("/version").build();
        LOGGER.debug("GET {}", uri);
        return client.target(uri).request().get(String.class);
    }

    /**
     * Get the user-agent for the client
     * 
     * @return user-agent for the client
     */
    public static String getUserAgent() {
        return "snowizard-client";
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
