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
package com.smoketurner.snowizard.application.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smoketurner.snowizard.api.Id;
import com.smoketurner.snowizard.api.protos.SnowizardProtos.SnowizardResponse;
import com.smoketurner.snowizard.application.exceptions.SnowizardExceptionMapper;
import com.smoketurner.snowizard.core.IdWorker;
import com.smoketurner.snowizard.exceptions.InvalidSystemClock;
import com.smoketurner.snowizard.exceptions.InvalidUserAgentError;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;
import io.dropwizard.testing.junit.ResourceTestRule;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Rule;
import org.junit.Test;

public class IdResourceTest {
  private static final String AGENT = "test-agent";
  private static final String AGENT_ERROR = "Invalid User-Agent header";
  private final IdWorker worker = mock(IdWorker.class);

  @Rule
  public final ResourceTestRule resources =
      ResourceTestRule.builder()
          .addProvider(new SnowizardExceptionMapper())
          .addProvider(new ProtocolBufferMessageBodyProvider())
          .addResource(new IdResource(worker))
          .build();

  @Test
  public void testGetIdAsString() throws Exception {
    final long expected = 100L;
    when(worker.getId(AGENT)).thenReturn(expected);

    final Response response =
        resources
            .client()
            .target("/")
            .request(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();
    final String entity = response.readEntity(String.class);
    final long actual = Long.valueOf(entity);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(actual).isEqualTo(expected);
    verify(worker).getId(AGENT);
  }

  @Test
  public void testGetIdAsStringInvalidAgent() throws Exception {
    when(worker.getId(AGENT)).thenThrow(new InvalidUserAgentError());

    final Response response =
        resources
            .client()
            .target("/")
            .request(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();
    final ErrorMessage message = response.readEntity(ErrorMessage.class);
    verify(worker).getId(AGENT);
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(message.getCode()).isEqualTo(400);
    assertThat(message.getMessage()).isEqualTo(AGENT_ERROR);
  }

  @Test
  public void testGetIdAsStringInvalidClock() throws Exception {
    when(worker.getId(AGENT)).thenThrow(new InvalidSystemClock());

    final Response response =
        resources
            .client()
            .target("/")
            .request(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();
    final ErrorMessage message = response.readEntity(ErrorMessage.class);
    verify(worker).getId(AGENT);
    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(message.getCode()).isEqualTo(500);
  }

  @Test
  public void testGetIdAsJSON() throws Exception {
    final long id = 100L;
    when(worker.getId(AGENT)).thenReturn(id);

    final Id actual =
        resources
            .client()
            .target("/")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get(Id.class);

    final Id expected = new Id(id);

    verify(worker).getId(AGENT);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testGetIdAsJSONInvalidAgent() throws Exception {
    when(worker.getId(AGENT)).thenThrow(new InvalidUserAgentError());

    final Response response =
        resources
            .client()
            .target("/")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();
    final ErrorMessage message = response.readEntity(ErrorMessage.class);
    verify(worker).getId(AGENT);
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(message.getCode()).isEqualTo(400);
    assertThat(message.getMessage()).isEqualTo(AGENT_ERROR);
  }

  @Test
  public void testGetIdAsJSONInvalidClock() throws Exception {
    when(worker.getId(AGENT)).thenThrow(new InvalidSystemClock());

    final Response response =
        resources
            .client()
            .target("/")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();
    final ErrorMessage message = response.readEntity(ErrorMessage.class);
    verify(worker).getId(AGENT);
    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(message.getCode()).isEqualTo(500);
  }

  @Test
  public void testGetIdAsJSONP() throws Exception {
    final long id = 100L;
    when(worker.getId(AGENT)).thenReturn(id);

    final String actual =
        resources
            .client()
            .target("/?callback=testing")
            .request("application/javascript")
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get(String.class);

    final Id expectedId = new Id(id);

    final ObjectMapper mapper = resources.getObjectMapper();
    final StringBuilder expected = new StringBuilder("testing(");
    expected.append(mapper.writeValueAsString(expectedId));
    expected.append(")");

    assertThat(actual).isEqualTo(expected.toString());
    verify(worker).getId(AGENT);
  }

  @Test
  public void testGetIdAsJSONPInvalidAgent() throws Exception {
    when(worker.getId(AGENT)).thenThrow(new InvalidUserAgentError());

    final Response response =
        resources
            .client()
            .target("/?callback=testing")
            .request("application/javascript")
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();
    final ErrorMessage message = response.readEntity(ErrorMessage.class);
    verify(worker).getId(AGENT);
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(message.getCode()).isEqualTo(400);
    assertThat(message.getMessage()).isEqualTo(AGENT_ERROR);
  }

  @Test
  public void testGetIdAsJSONPInvalidClock() throws Exception {
    when(worker.getId(AGENT)).thenThrow(new InvalidSystemClock());

    final Response response =
        resources
            .client()
            .target("/?callback=testing")
            .request("application/javascript")
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();
    final ErrorMessage message = response.readEntity(ErrorMessage.class);
    verify(worker).getId(AGENT);
    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(message.getCode()).isEqualTo(500);
  }

  @Test
  public void testGetIdAsProtobuf() throws Exception {
    final long id = 100L;
    when(worker.getId(AGENT)).thenReturn(id);

    final Response response =
        resources
            .client()
            .target("/")
            .register(new ProtocolBufferMessageBodyProvider())
            .request(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();

    final SnowizardResponse actual = response.readEntity(SnowizardResponse.class);

    final SnowizardResponse expected = SnowizardResponse.newBuilder().addId(id).build();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(actual).isEqualTo(expected);
    verify(worker).getId(AGENT);
  }

  @Test
  public void testGetIdAsProtobufEmptyCount() throws Exception {
    final long id = 100L;
    when(worker.getId(AGENT)).thenReturn(id);

    final Response response =
        resources
            .client()
            .target("/")
            .register(new ProtocolBufferMessageBodyProvider())
            .queryParam("count", "")
            .request(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();

    final SnowizardResponse actual = response.readEntity(SnowizardResponse.class);

    final SnowizardResponse expected = SnowizardResponse.newBuilder().addId(id).build();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(actual).isEqualTo(expected);
    verify(worker).getId(AGENT);
  }

  @Test
  public void testGetIdAsProtobufInvalidCount() throws Exception {
    when(worker.getId(AGENT)).thenReturn(100L);

    final Response response =
        resources
            .client()
            .target("/")
            .register(new ProtocolBufferMessageBodyProvider())
            .queryParam("count", "test")
            .request(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();

    assertThat(response.getStatus()).isEqualTo(400);
    verify(worker, never()).getId(AGENT);
  }

  @Test
  public void testGetIdAsProtobufNullCount() throws Exception {
    final long id = 100L;
    when(worker.getId(AGENT)).thenReturn(id);

    final String count = null;
    final Response response =
        resources
            .client()
            .target("/")
            .register(new ProtocolBufferMessageBodyProvider())
            .queryParam("count", count)
            .request(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();

    final SnowizardResponse actual = response.readEntity(SnowizardResponse.class);

    final SnowizardResponse expected = SnowizardResponse.newBuilder().addId(id).build();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(actual).isEqualTo(expected);
    verify(worker).getId(AGENT);
  }

  @Test
  public void testGetIdAsProtobufInvalidAgent() throws Exception {
    when(worker.getId(AGENT)).thenThrow(new InvalidUserAgentError());

    final Response response =
        resources
            .client()
            .target("/")
            .request(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();
    final ErrorMessage message = response.readEntity(ErrorMessage.class);
    verify(worker).getId(AGENT);
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(message.getCode()).isEqualTo(400);
    assertThat(message.getMessage()).isEqualTo(AGENT_ERROR);
  }

  @Test
  public void testGetIdAsProtobufInvalidClock() throws Exception {
    when(worker.getId(AGENT)).thenThrow(new InvalidSystemClock());

    final Response response =
        resources
            .client()
            .target("/")
            .request(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get();
    final ErrorMessage message = response.readEntity(ErrorMessage.class);
    verify(worker).getId(AGENT);
    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(message.getCode()).isEqualTo(500);
  }
}
