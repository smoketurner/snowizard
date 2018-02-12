/**
 * Copyright 2017 Coda Hale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.snowizard.grpc.stats;

import javax.annotation.concurrent.Immutable;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public Snapshot(long count, double throughput, double concurrency,
            double latency, double p50, double p90, double p99, double p999,
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
