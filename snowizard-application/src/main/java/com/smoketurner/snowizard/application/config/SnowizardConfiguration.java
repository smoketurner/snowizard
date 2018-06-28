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
package com.smoketurner.snowizard.application.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.dropwizard.zipkin.ConsoleZipkinFactory;
import com.smoketurner.dropwizard.zipkin.ZipkinFactory;
import io.dropwizard.Configuration;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class SnowizardConfiguration extends Configuration {
  private static final int MAX_ID = 1024;

  @Min(1)
  @Max(MAX_ID)
  private int workerId = 1;

  @Min(1)
  @Max(MAX_ID)
  private int datacenterId = 1;

  private boolean validateUserAgent = false;

  @Valid @NotNull private final ZipkinFactory zipkin = new ConsoleZipkinFactory();

  @JsonProperty("worker_id")
  public int getWorkerId() {
    return workerId;
  }

  @JsonProperty("worker_id")
  public void setWorkerId(final int workerId) {
    this.workerId = workerId;
  }

  @JsonProperty("datacenter_id")
  public int getDatacenterId() {
    return datacenterId;
  }

  @JsonProperty("datacenter_id")
  public void setDatacenterId(final int datacenterId) {
    this.datacenterId = datacenterId;
  }

  @JsonProperty("validate_user_agent")
  public boolean validateUserAgent() {
    return validateUserAgent;
  }

  @JsonProperty("validate_user_agent")
  public void setValidateUserAgent(boolean validateUserAgent) {
    this.validateUserAgent = validateUserAgent;
  }

  @JsonProperty
  public ZipkinFactory getZipkin() {
    return zipkin;
  }
}
