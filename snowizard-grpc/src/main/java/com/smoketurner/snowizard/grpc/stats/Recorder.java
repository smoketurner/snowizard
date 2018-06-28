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

import java.util.concurrent.TimeUnit;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Recorder {

  private static final Logger LOGGER = LoggerFactory.getLogger(Recorder.class);

  private final IntervalAdder count;
  private final IntervalAdder responseTime;
  private final org.HdrHistogram.Recorder latency;
  private final long goalLatency;
  private volatile Histogram histogram;

  /**
   * Constructor
   *
   * @param minLatency
   * @param maxLatency
   * @param goalLatency
   * @param latencyUnit
   */
  public Recorder(long minLatency, long maxLatency, long goalLatency, TimeUnit latencyUnit) {

    this.goalLatency = latencyUnit.toMicros(goalLatency);
    this.count = new IntervalAdder();
    this.responseTime = new IntervalAdder();
    this.latency =
        new org.HdrHistogram.Recorder(
            latencyUnit.toMicros(minLatency), latencyUnit.toMicros(maxLatency), 1);
    this.histogram = latency.getIntervalHistogram(); // preload reporting
    // histogram
  }

  public void record(long startNanoTime) {
    final long duration = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startNanoTime);
    count.add(1);
    responseTime.add(duration);
    try {
      latency.recordValue(duration);
    } catch (ArrayIndexOutOfBoundsException ignored) {
      LOGGER.warn("Very slow value: {}us", duration);
    }
  }

  public Snapshot interval() {
    final IntervalCount requestCount = count.interval();
    final IntervalCount responseTimeCount = responseTime.interval();
    final Histogram h = latency.getIntervalHistogram(histogram);
    final long c = requestCount.getCount();
    final double x = requestCount.getRate();
    final long satisfied = h.getCountBetweenValues(0, goalLatency);
    final long tolerating = h.getCountBetweenValues(goalLatency, goalLatency * 4);
    final double p50 = h.getValueAtPercentile(50) * 1e-6;
    final double p90 = h.getValueAtPercentile(90) * 1e-6;
    final double p99 = h.getValueAtPercentile(99) * 1e-6;
    final double p999 = h.getValueAtPercentile(99.9) * 1e-6;
    this.histogram = h;
    final double r, n, apdex;
    if (c == 0) {
      r = n = apdex = 0;
    } else {
      r = responseTimeCount.getRate() / c * 1e-6;
      n = x * r;
      apdex = Math.min(1.0, (satisfied + (tolerating / 2.0)) / c);
    }
    return new Snapshot(c, x, n, r, p50, p90, p99, p999, apdex);
  }
}
