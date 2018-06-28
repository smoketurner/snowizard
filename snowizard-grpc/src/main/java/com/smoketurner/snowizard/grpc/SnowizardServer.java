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
package com.smoketurner.snowizard.grpc;

import com.smoketurner.snowizard.core.IdWorker;
import com.smoketurner.snowizard.grpc.util.Netty;
import com.smoketurner.snowizard.grpc.util.StatsTracerFactory;
import com.smoketurner.snowizard.grpc.util.TlsContext;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnowizardServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(SnowizardServer.class);

  private final EventLoopGroup bossEventLoopGroup;
  private final EventLoopGroup workerEventLoopGroup;
  private final Server server;
  private final StatsTracerFactory stats;

  /**
   * Constructor
   *
   * @param port Port to listen on
   * @param tls TLS context
   * @param workerId Worker ID
   * @param datacenterId Datacenter ID
   * @throws SSLException
   */
  private SnowizardServer(int port, TlsContext tls, long workerId, long datacenterId)
      throws SSLException {

    this.stats = new StatsTracerFactory();
    this.bossEventLoopGroup = Netty.newBossEventLoopGroup();
    this.workerEventLoopGroup = Netty.newWorkerEventLoopGroup();

    final IdWorker worker = IdWorker.builder(workerId, datacenterId).build();

    this.server =
        NettyServerBuilder.forPort(port)
            .bossEventLoopGroup(bossEventLoopGroup)
            .workerEventLoopGroup(workerEventLoopGroup)
            .channelType(Netty.serverChannelType())
            .addStreamTracerFactory(stats)
            .sslContext(tls.toServerContext())
            .addService(new SnowizardImpl(worker))
            .build();
  }

  private void start() throws IOException, InterruptedException {
    stats.start();
    server.start();
    LOGGER.info("Server started, listening on {}", server.getPort());
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    server.awaitTermination();
  }

  private void stop() {
    stats.stop();
    if (!server.isShutdown()) {
      server.shutdown();
    }
    bossEventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    workerEventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
  }

  @Command(name = "server", description = "Run a gRPC Snowizard service.")
  public static class Cmd implements Runnable {

    @Option(
        name = {"-p", "--port"},
        description = "the port to listen on")
    private int port = 8080;

    @Option(
        name = {"-w", "--worker-id"},
        description = "worker ID")
    private long workerId = 1L;

    @Option(
        name = {"-d", "--datacenter-id"},
        description = "datacenter ID")
    private long datacenterId = 1L;

    @Option(name = "--ca-certs")
    private String trustedCertsPath = "cert.crt";

    @Option(name = "--cert")
    private String certPath = "cert.crt";

    @Option(name = "--key")
    private String keyPath = "cert.key";

    @Override
    public void run() {
      try {
        final TlsContext tls = new TlsContext(trustedCertsPath, certPath, keyPath);
        final SnowizardServer server = new SnowizardServer(port, tls, workerId, datacenterId);
        server.start();
      } catch (IOException | InterruptedException e) {
        LOGGER.error("Error running command", e);
      }
    }
  }
}
