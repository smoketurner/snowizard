/*
 * Copyright © 2013, General Electric Corporation
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.smoketurner.snowizard.client;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.smoketurner.snowizard.api.protos.SnowizardProtos.SnowizardResponse;
import com.smoketurner.snowizard.client.exceptions.SnowizardClientException;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnowizardClient implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SnowizardClient.class);
  private static final String PING_RESPONSE = "pong";
  private final Timer fetchTimer;
  private final Client client;
  private final URI rootUri;

  /**
   * Constructor
   *
   * @param registry MetricRegistry
   * @param client Jersey client
   * @param uri API endpoint
   */
  public SnowizardClient(final MetricRegistry registry, final Client client, final URI uri) {
    this.client = Objects.requireNonNull(client);
    this.rootUri = Objects.requireNonNull(uri);
    this.fetchTimer = registry.timer(name(SnowizardClient.class, "fetch"));
  }

  /**
   * Execute a request to the Snowizard service URL
   *
   * @param count Number of IDs to generate
   * @return SnowizardResponse
   * @throws IOException Error in communicating with Snowizard
   */
  private SnowizardResponse executeRequest(final int count) {
    final URI uri = UriBuilder.fromUri(rootUri).path("/").queryParam("count", count).build();
    LOGGER.debug("GET {}", uri);
    try (Timer.Context context = fetchTimer.time()) {
      return client
          .target(uri)
          .request(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
          .header(HttpHeaders.USER_AGENT, getUserAgent())
          .get(SnowizardResponse.class);
    }
  }

  /**
   * Get a new ID from Snowizard
   *
   * @return generated ID
   * @throws SnowizardClientException when unable to get an ID from any host
   */
  public long getId() throws SnowizardClientException {
    try {
      final SnowizardResponse snowizard = executeRequest(1);
      return snowizard.getId(0);
    } catch (final Exception e) {
      LOGGER.warn("Unable to get ID from host ({})", rootUri);
      throw new SnowizardClientException("Unable to generate ID from Snowizard", e);
    }
  }

  /**
   * Get multiple IDs from Snowizard
   *
   * @param count Number of IDs to return
   * @return generated IDs
   * @throws SnowizardClientException when unable to get an ID from any host
   */
  public List<Long> getIds(final int count) throws SnowizardClientException {
    try {
      final SnowizardResponse snowizard = executeRequest(count);
      return snowizard.getIdList();
    } catch (final Exception e) {
      LOGGER.warn("Unable to get ID from host ({})", rootUri);
      throw new SnowizardClientException("Unable to generate batch of IDs from Snowizard", e);
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
