/**
 * Copyright 2018 Smoke Turner, LLC
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
package com.smoketurner.snowizard.grpc;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableList;
import com.smoketurner.snowizard.grpc.protos.SnowizardGrpc;
import com.smoketurner.snowizard.grpc.protos.SnowizardRequest;
import com.smoketurner.snowizard.grpc.protos.SnowizardResponse;
import com.smoketurner.snowizard.grpc.stats.Recorder;
import com.smoketurner.snowizard.grpc.stats.Snapshot;
import com.smoketurner.snowizard.grpc.util.Netty;
import com.smoketurner.snowizard.grpc.util.TlsContext;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.EventLoopGroup;
import net.logstash.logback.marker.Markers;

public class SnowizardClient {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SnowizardClient.class);

    private final EventLoopGroup eventLoopGroup;
    private final ManagedChannel channel;
    private final SnowizardGrpc.SnowizardBlockingStub blockingStub;

    /**
     * Constructor
     *
     * @param host
     *            Server host
     * @param port
     *            Server port
     * @param tls
     *            TLS context
     */
    private SnowizardClient(String host, int port, TlsContext tls)
            throws SSLException {

        this.eventLoopGroup = Netty.newWorkerEventLoopGroup();
        this.channel = NettyChannelBuilder.forAddress(host, port)
                .eventLoopGroup(eventLoopGroup)
                .channelType(Netty.clientChannelType())
                .sslContext(tls.toClientContext()).build();
        this.blockingStub = SnowizardGrpc.newBlockingStub(channel);
    }

    private void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    }

    private OptionalLong getId() {
        LOGGER.debug("Requesting 1 id");
        final SnowizardResponse response;
        try {
            response = blockingStub.getId(null);
        } catch (StatusRuntimeException e) {
            LOGGER.warn("RPC failed: {}", e.getStatus());
            return OptionalLong.empty();
        }
        return OptionalLong.of(response.getId(0));
    }

    private LongStream getIds(final int count) {
        LOGGER.debug("Requesting {} ids", count);
        final SnowizardRequest request = SnowizardRequest.newBuilder()
                .setCount(count).build();

        final Iterator<SnowizardResponse> response;
        try {
            response = blockingStub.getIds(request);
        } catch (StatusRuntimeException e) {
            LOGGER.warn("RPC failed: {}", e.getStatus());
            return LongStream.empty();
        }

        // consume the iterator so the request can be completed
        final ImmutableList<SnowizardResponse> responses = ImmutableList
                .copyOf(response);

        return responses.stream()
                .flatMapToLong(r -> r.getIdList().stream().mapToLong(l -> l))
                .sorted();
    }

    @Command(name = "client", description = "Run a GRPC Snowizard client.")
    public static class Cmd implements Runnable {

        @Option(name = { "-h",
                "--hostname" }, description = "the hostname of the gRPC server")
        private String hostname = "localhost";

        @Option(name = { "-p",
                "--port" }, description = "the port of the gRPC server")
        private int port = 8080;

        @Option(name = { "-n",
                "--requests" }, description = "the number of requests to make")
        private int requests = 1_000_000;

        @Option(name = { "-f",
                "--fetch" }, description = "the number of IDs to fetch per request")
        private int fetch = 1;

        @Option(name = { "-c",
                "--threads" }, description = "the number of threads to use")
        private int threads = 10;

        @Option(name = "--ca-certs")
        private String trustedCertsPath = "cert.crt";

        @Option(name = "--cert")
        private String certPath = "cert.crt";

        @Option(name = "--key")
        private String keyPath = "cert.key";

        @Override
        public void run() {
            try {
                final TlsContext tls = new TlsContext(trustedCertsPath,
                        certPath, keyPath);
                final SnowizardClient client = new SnowizardClient(hostname,
                        port, tls);
                try {
                    final Recorder recorder = new Recorder(500,
                            TimeUnit.MINUTES.toMicros(1),
                            TimeUnit.MILLISECONDS.toMicros(10),
                            TimeUnit.MICROSECONDS);
                    LOGGER.info("Initial request: {}", client.getId());
                    LOGGER.info("Sending {} requests from {} threads", requests,
                            threads);

                    final ExecutorService threadPool = Executors
                            .newFixedThreadPool(threads);
                    final Instant start = Instant.now();
                    for (int i = 0; i < threads; i++) {
                        threadPool.execute(() -> {
                            for (int j = 0; j < requests / threads; j++) {
                                final long t = System.nanoTime();
                                client.getIds(fetch).close();
                                // client.getId();
                                recorder.record(t);
                            }
                        });
                    }
                    threadPool.shutdown();
                    threadPool.awaitTermination(20, TimeUnit.MINUTES);

                    final Snapshot stats = recorder.interval();
                    final Duration duration = Duration.between(start,
                            Instant.now());
                    LOGGER.info(
                            Markers.append("stats", stats)
                                    .and(Markers.append("duration",
                                            duration.toString())),
                            "{} requests in {} ({} req/sec)", stats.getCount(),
                            duration, stats.getThroughput());
                } finally {
                    client.shutdown();
                }
            } catch (SSLException | InterruptedException e) {
                LOGGER.error("Error running command", e);
            }
        }
    }
}
