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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Optional;
import com.smoketurner.snowizard.api.protos.SnowizardProtos.SnowizardResponse;
import io.dropwizard.jersey.params.IntParam;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;
import io.dropwizard.testing.junit.DropwizardClientRule;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class SnowizardClientTest {

  @Path("/")
  public static class IdResource {
    @GET
    @Produces(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
    public SnowizardResponse fetch(
        @HeaderParam(HttpHeaders.USER_AGENT) final String agent,
        @QueryParam("count") final Optional<IntParam> count) {
      final SnowizardResponse.Builder builder = SnowizardResponse.newBuilder();
      if (count.isPresent()) {
        for (int i = 1; i <= count.get().get(); i++) {
          builder.addId(i);
        }
      }
      return builder.build();
    }
  }

  @Path("/ping")
  public static class PingResource {
    @GET
    public String ping() {
      return "pong";
    }
  }

  @Path("/version")
  public static class VersionResource {
    @GET
    public String version() {
      return "1.0.0";
    }
  }

  @ClassRule
  public static final DropwizardClientRule resources =
      new DropwizardClientRule(
          new ProtocolBufferMessageBodyProvider(), new IdResource(),
          new PingResource(), new VersionResource());

  private static SnowizardClient client;

  @BeforeClass
  public static void setUp() {
    final SnowizardClientBuilder builder = new SnowizardClientBuilder(resources.getEnvironment());
    final SnowizardClientConfiguration configuration = new SnowizardClientConfiguration();
    configuration.setUri(resources.baseUri());
    client = builder.build(configuration);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    client.close();
  }

  @Test
  public void testGetId() throws Exception {
    final long actual = client.getId();
    assertThat(actual).isGreaterThan(0);
  }

  @Test
  public void testPing() throws Exception {
    assertThat(client.ping()).isTrue();
  }

  @Test
  public void testVersion() throws Exception {
    assertThat(client.version()).isEqualTo("1.0.0");
  }
}
