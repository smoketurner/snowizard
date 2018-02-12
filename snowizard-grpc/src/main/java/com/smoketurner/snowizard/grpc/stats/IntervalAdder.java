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

import java.util.concurrent.atomic.LongAdder;

public class IntervalAdder {

    private final LongAdder count;
    private volatile long timestamp;

    public IntervalAdder() {
        this.count = new LongAdder();
        this.timestamp = System.nanoTime();
    }

    public void add(long x) {
        count.add(x);
    }

    public IntervalCount interval() {
        final long n = count.sumThenReset();
        final long t = System.nanoTime();
        final double i = (t - timestamp) * 1e-9;
        this.timestamp = t;

        return new IntervalCount(n / i, n);
    }
}