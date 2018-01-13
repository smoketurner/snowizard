/*
 * Copyright 2010-2012 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.snowizard.core;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.smoketurner.snowizard.exceptions.InvalidSystemClock;
import com.smoketurner.snowizard.exceptions.InvalidUserAgentError;

public class IdWorker {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(IdWorker.class);
    private static final Pattern AGENT_PATTERN = Pattern
            .compile("([a-zA-Z][a-zA-Z0-9\\-]*)");

    public static final long TWEPOCH = 1288834974657L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = -1L
            ^ (-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS
            + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS
            + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);

    private final MetricRegistry registry;
    private final Counter idsCounter;
    private final Counter exceptionsCounter;
    private final Map<String, Counter> agentCounters = new ConcurrentHashMap<>();
    private final long workerId;
    private final long datacenterId;
    private final boolean validateUserAgent;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    /**
     * Constructor
     * 
     * Used by the tests
     * 
     * @param workerId
     * @param datacenterId
     */
    protected IdWorker(final long workerId, final long datacenterId) {
        this(builder(workerId, datacenterId));
    }

    /**
     * Constructor
     * 
     * @param builder
     */
    private IdWorker(final Builder builder) {

        exceptionsCounter = builder.registry
                .counter(MetricRegistry.name(IdWorker.class, "exceptions"));
        idsCounter = builder.registry
                .counter(MetricRegistry.name(IdWorker.class, "ids_generated"));

        if (builder.workerId > MAX_WORKER_ID || builder.workerId < 0) {
            exceptionsCounter.inc();
            throw new IllegalArgumentException(String.format(
                    "worker Id can't be greater than %d or less than 0",
                    MAX_WORKER_ID));
        }
        if (builder.datacenterId > MAX_DATACENTER_ID
                || builder.datacenterId < 0) {
            exceptionsCounter.inc();
            throw new IllegalArgumentException(String.format(
                    "datacenter Id can't be greater than %d or less than 0",
                    MAX_DATACENTER_ID));
        }

        this.workerId = builder.workerId;
        this.datacenterId = builder.datacenterId;
        this.validateUserAgent = builder.validateUserAgent;
        this.registry = builder.registry;
        this.sequence = builder.startSequence;

        LOGGER.info(
                "worker starting. timestamp left shift {}, datacenter id bits {}, worker id bits {}, sequence bits {}, workerid {}",
                TIMESTAMP_LEFT_SHIFT, DATACENTER_ID_BITS, WORKER_ID_BITS,
                SEQUENCE_BITS, builder.workerId);
    }

    public static final Builder builder(final long workerId,
            final long datacenterId) {
        return new Builder(workerId, datacenterId);
    }

    public static final class Builder {
        private final long workerId;
        private final long datacenterId;
        private long startSequence = 0L;
        private boolean validateUserAgent = true;
        private MetricRegistry registry = new MetricRegistry();

        /**
         * Constructor
         *
         * @param workerId
         * @param datacenterId
         */
        public Builder(final long workerId, final long datacenterId) {
            this.workerId = workerId;
            this.datacenterId = datacenterId;
        }

        public Builder withStartSequence(final long startSequence) {
            this.startSequence = startSequence;
            return this;
        }

        public Builder withValidateUserAgent(final boolean validateUserAgent) {
            this.validateUserAgent = validateUserAgent;
            return this;
        }

        public Builder withMetricRegistry(final MetricRegistry registry) {
            this.registry = Objects.requireNonNull(registry);
            return this;
        }

        public IdWorker build() {
            return new IdWorker(this);
        }
    }

    /**
     * Get the next ID for a given user-agent
     * 
     * @param agent
     *            User Agent
     * @return Generated ID
     * @throws InvalidUserAgentError
     *             When the user agent is invalid
     * @throws InvalidSystemClock
     *             When the system clock is moving backward
     */
    public long getId(final String agent)
            throws InvalidUserAgentError, InvalidSystemClock {
        if (!isValidUserAgent(agent)) {
            exceptionsCounter.inc();
            throw new InvalidUserAgentError();
        }

        final long id = nextId();
        genCounter(agent);

        return id;
    }

    /**
     * Return the worker ID
     * 
     * @return Worker ID
     */
    public long getWorkerId() {
        return workerId;
    }

    /**
     * Return the data center ID
     * 
     * @return Datacenter ID
     */
    public long getDatacenterId() {
        return datacenterId;
    }

    /**
     * Return the current system time in milliseconds.
     * 
     * @return Current system time in milliseconds
     */
    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Return the current sequence position (visible for testing)
     * 
     * @return Current sequence position
     */
    public long getSequence() {
        return sequence;
    }

    /**
     * Set the sequence to a given value (visible for testing)
     * 
     * @param value
     *            New sequence value
     */
    public void setSequence(final long value) {
        sequence = value;
    }

    /**
     * Get the next ID
     * 
     * @return Next ID
     * @throws InvalidSystemClock
     *             When the clock is moving backward
     */
    public synchronized long nextId() throws InvalidSystemClock {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            exceptionsCounter.inc();
            LOGGER.error(
                    "clock is moving backwards. Rejecting requests until {}",
                    lastTimestamp);
            throw new InvalidSystemClock(String.format(
                    "Clock moved backwards. Refusing to generate id for %d milliseconds",
                    (lastTimestamp - timestamp)));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        final long id = ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT) | sequence;

        return id;
    }

    /**
     * Return the next time in milliseconds
     * 
     * @param lastTimestamp
     *            Last timestamp
     * @return Next timestamp in milliseconds
     */
    protected long tilNextMillis(final long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * Generate a new timestamp (currently in milliseconds)
     * 
     * @return current timestamp in milliseconds
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * Check whether the user agent is valid
     * 
     * @param agent
     *            User-Agent
     * @return True if the user agent is valid
     */
    public boolean isValidUserAgent(final String agent) {
        if (!validateUserAgent) {
            return true;
        }
        final Matcher matcher = AGENT_PATTERN.matcher(agent);
        return matcher.matches();
    }

    /**
     * Update the counters for a given user agent
     * 
     * @param agent
     *            User-Agent
     */
    protected void genCounter(final String agent) {
        idsCounter.inc();
        if (!agentCounters.containsKey(agent)) {
            agentCounters.put(agent, registry.counter(MetricRegistry
                    .name(IdWorker.class, "ids_generated_" + agent)));
        }
        agentCounters.get(agent).inc();
    }
}
