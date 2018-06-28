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

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Netty {

  private static final Logger LOGGER = LoggerFactory.getLogger(Netty.class);
  private static final int WORKER_THREADS = Runtime.getRuntime().availableProcessors() * 2;

  static {
    if (Epoll.isAvailable()) {
      LOGGER.info("Using epoll");
    } else {
      LOGGER.info("Using java.nio");
    }
  }

  public static EventLoopGroup newBossEventLoopGroup() {
    if (Epoll.isAvailable()) {
      return new EpollEventLoopGroup();
    }
    return new NioEventLoopGroup();
  }

  public static EventLoopGroup newWorkerEventLoopGroup() {
    if (Epoll.isAvailable()) {
      return new EpollEventLoopGroup(WORKER_THREADS);
    }
    return new NioEventLoopGroup(WORKER_THREADS);
  }

  public static Class<? extends ServerChannel> serverChannelType() {
    if (Epoll.isAvailable()) {
      return EpollServerSocketChannel.class;
    }
    return NioServerSocketChannel.class;
  }

  public static Class<? extends Channel> clientChannelType() {
    if (Epoll.isAvailable()) {
      return EpollSocketChannel.class;
    }
    return NioSocketChannel.class;
  }
}
