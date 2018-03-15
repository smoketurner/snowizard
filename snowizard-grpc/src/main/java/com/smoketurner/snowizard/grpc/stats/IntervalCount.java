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
public class IntervalCount {

    private final double rate;
    private final long count;

    public IntervalCount(final double rate, final long count) {
        this.rate = rate;
        this.count = count;
    }

    @JsonProperty
    public double getRate() {
        return rate;
    }

    @JsonProperty
    public long getCount() {
        return count;
    }
}