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
package com.smoketurner.snowizard.grpc.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Snapshot {

  private final long count;
  private final double throughput;
  private final double concurrency;
  private final double latency;
  private final double p50;
  private final double p90;
  private final double p99;
  private final double p999;
  private final double apdex;

  public Snapshot(
      long count,
      double throughput,
      double concurrency,
      double latency,
      double p50,
      double p90,
      double p99,
      double p999,
      double apdex) {
    this.count = count;
    this.throughput = throughput;
    this.concurrency = concurrency;
    this.latency = latency;
    this.p50 = p50;
    this.p90 = p90;
    this.p99 = p99;
    this.p999 = p999;
    this.apdex = apdex;
  }

  @JsonProperty
  public long getCount() {
    return count;
  }

  @JsonProperty
  public double getThroughput() {
    return throughput;
  }

  @JsonProperty
  public double getConcurrency() {
    return concurrency;
  }

  @JsonProperty
  public double getLatency() {
    return latency;
  }

  @JsonProperty
  public double getP50() {
    return p50;
  }

  @JsonProperty
  public double getP90() {
    return p90;
  }

  @JsonProperty
  public double getP99() {
    return p99;
  }

  @JsonProperty
  public double getP999() {
    return p999;
  }

  @JsonProperty
  public double getApdex() {
    return apdex;
  }
}
