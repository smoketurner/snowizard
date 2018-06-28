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

import com.codahale.metrics.annotation.Timed;
import com.smoketurner.snowizard.api.Id;
import com.smoketurner.snowizard.api.protos.SnowizardProtos.SnowizardResponse;
import com.smoketurner.snowizard.application.exceptions.SnowizardException;
import com.smoketurner.snowizard.core.IdWorker;
import com.smoketurner.snowizard.exceptions.InvalidSystemClock;
import com.smoketurner.snowizard.exceptions.InvalidUserAgentError;
import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.params.IntParam;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class IdResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdResource.class);
  private final IdWorker worker;

  /**
   * Constructor
   *
   * @param worker ID worker
   */
  public IdResource(final IdWorker worker) {
    this.worker = Objects.requireNonNull(worker);
  }

  /**
   * Get a new ID and handle any thrown exceptions
   *
   * @param agent User Agent
   * @return generated ID
   * @throws SnowizardException if invalid agent or clock
   */
  public long getId(final String agent) {
    try {
      return worker.getId(agent);
    } catch (final InvalidUserAgentError e) {
      LOGGER.error("Invalid user agent ({})", agent);
      throw new SnowizardException(Response.Status.BAD_REQUEST, "Invalid User-Agent header", e);
    } catch (final InvalidSystemClock e) {
      LOGGER.error("Invalid system clock", e);
      throw new SnowizardException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage(), e);
    }
  }

  /**
   * Get a new ID as plain text
   *
   * @param agent User Agent
   * @return generated ID
   */
  @GET
  @Timed
  @Produces(MediaType.TEXT_PLAIN)
  @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
  public String getIdAsString(@HeaderParam(HttpHeaders.USER_AGENT) final @NotEmpty String agent) {
    return String.valueOf(getId(agent));
  }

  /**
   * Get a new ID as JSON
   *
   * @param agent User Agent
   * @return generated ID
   */
  @GET
  @Timed
  @JSONP(callback = "callback", queryParam = "callback")
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
  public Id getIdAsJSON(@HeaderParam(HttpHeaders.USER_AGENT) final @NotEmpty String agent) {
    return new Id(getId(agent));
  }

  /**
   * Get one or more IDs as a Google Protocol Buffer response
   *
   * @param agent User Agent
   * @param count Number of IDs to return
   * @return generated IDs
   */
  @GET
  @Timed
  @Produces(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
  @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
  public SnowizardResponse getIdAsProtobuf(
      @HeaderParam(HttpHeaders.USER_AGENT) final @NotEmpty String agent,
      @QueryParam("count") @DefaultValue("1") final IntParam count) {

    final List<Long> ids = new ArrayList<>();
    if (count != null) {
      for (int i = 0; i < count.get(); i++) {
        ids.add(getId(agent));
      }
    } else {
      ids.add(getId(agent));
    }
    return SnowizardResponse.newBuilder().addAllId(ids).build();
  }
}
