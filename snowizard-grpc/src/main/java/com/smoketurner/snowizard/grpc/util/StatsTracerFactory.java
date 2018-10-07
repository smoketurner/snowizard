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
package com.smoketurner.snowizard.grpc.util;

import com.smoketurner.snowizard.grpc.stats.IntervalAdder;
import com.smoketurner.snowizard.grpc.stats.Recorder;
import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;
import io.grpc.Status;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stream tracer factory which measures throughput, concurrency, response time, and latency
 * distribution.
 */
public class StatsTracerFactory extends ServerStreamTracer.Factory {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatsTracerFactory.class);
  private static final long MIN_DURATION = TimeUnit.MICROSECONDS.toMicros(500);
  private static final long GOAL_DURATION = TimeUnit.MILLISECONDS.toMicros(10);
  private static final long MAX_DURATION = TimeUnit.SECONDS.toMicros(30);

  private final IntervalAdder bytesIn = new IntervalAdder();
  private final IntervalAdder bytesOut = new IntervalAdder();
  private final Recorder all = newRecorder();
  private final ConcurrentMap<String, Recorder> endpoints = new ConcurrentHashMap<>();

  @Nullable private ScheduledExecutorService executor;

  @Override
  public ServerStreamTracer newServerStreamTracer(String fullMethodName, Metadata headers) {

    return new ServerStreamTracer() {
      final long start = System.nanoTime();
      final Recorder endpoint = endpoints.computeIfAbsent(fullMethodName, k -> newRecorder());

      @Override
      public void outboundWireSize(long bytes) {
        bytesOut.add(bytes);
      }

      @Override
      public void inboundWireSize(long bytes) {
        bytesIn.add(bytes);
      }

      @Override
      public void streamClosed(Status status) {
        final double duration = (System.nanoTime() - start) * 1e-9;
        LOGGER.debug(
            Markers.append("grpc_method_name", fullMethodName)
                .and(Markers.append("status", status))
                .and(Markers.append("duration", duration)),
            "request handled");
        all.record(start);
        endpoint.record(start);
      }
    };
  }

  public void start() {
    executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(this::report, 1, 1, TimeUnit.SECONDS);
  }

  public void stop() {
    if (executor != null) {
      executor.shutdown();
    }
  }

  /**
   * Calculate and report the three parameters of Little's Law and some latency percentiles.
   *
   * <p>This just writes them to stdout, but presumably we'd be reporting them to a centralized
   * service.
   */
  private void report() {
    LogstashMarker marker =
        Markers.append("all", all.interval())
            .and(Markers.append("bytes_in", bytesIn.interval()))
            .and(Markers.append("bytes_out", bytesOut.interval()));
    for (Entry<String, Recorder> entry : endpoints.entrySet()) {
      marker = marker.and(Markers.append(entry.getKey(), entry.getValue().interval()));
    }
    LOGGER.info(marker, "stats");
  }

  private Recorder newRecorder() {
    return new Recorder(MIN_DURATION, MAX_DURATION, GOAL_DURATION, TimeUnit.MICROSECONDS);
  }
}
