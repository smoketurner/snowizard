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

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;
import io.dropwizard.setup.Environment;
import java.util.Objects;
import javax.ws.rs.client.Client;

public class SnowizardClientBuilder {
  private final Environment environment;

  /**
   * Constructor
   *
   * @param environment Environment
   */
  public SnowizardClientBuilder(final Environment environment) {
    this.environment = Objects.requireNonNull(environment);
  }

  /**
   * Build a new {@link SnowizardClient}
   *
   * @param configuration Configuration to use for the client
   * @return new SnowizardClient
   */
  public SnowizardClient build(final SnowizardClientConfiguration configuration) {
    final Client client =
        new JerseyClientBuilder(environment)
            .using(configuration)
            .withProvider(ProtocolBufferMessageBodyProvider.class)
            .build("snowizard");
    return build(configuration, client);
  }

  /**
   * Build a new {@link SnowizardClient}. If using this method instead of {@link
   * #build(SnowizardClientConfiguration)}, remember to register the {@link
   * ProtocolBufferMessageBodyProvider}.
   *
   * @param configuration Configuration to use for the client
   * @param client Jersey Client to use
   * @return new SnowizardClient
   */
  public SnowizardClient build(
      final SnowizardClientConfiguration configuration, final Client client) {
    return new SnowizardClient(environment.metrics(), client, configuration.getUri());
  }
}
