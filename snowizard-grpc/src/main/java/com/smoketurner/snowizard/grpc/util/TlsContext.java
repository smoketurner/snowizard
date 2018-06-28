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

import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.io.File;
import javax.net.ssl.SSLException;

public class TlsContext {

  private final File trustedCerts;
  private final File cert;
  private final File key;

  /**
   * Constructor
   *
   * @param trustedCertsPath
   * @param certPath
   * @param keyPath
   */
  public TlsContext(String trustedCertsPath, String certPath, String keyPath) {

    this.trustedCerts = new File(trustedCertsPath);
    if (!trustedCerts.exists()) {
      throw new IllegalArgumentException("Can't find " + trustedCertsPath);
    }

    this.cert = new File(certPath);
    if (!cert.exists()) {
      throw new IllegalArgumentException("Can't find " + certPath);
    }

    this.key = new File(keyPath);
    if (!key.exists()) {
      throw new IllegalArgumentException("Can't find " + keyPath);
    }
  }

  public SslContext toClientContext() throws SSLException {
    return GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
        .trustManager(trustedCerts)
        .keyManager(cert, key)
        .build();
  }

  public SslContext toServerContext() throws SSLException {
    return GrpcSslContexts.configure(SslContextBuilder.forServer(cert, key), SslProvider.OPENSSL)
        .trustManager(trustedCerts)
        .clientAuth(ClientAuth.REQUIRE)
        .build();
  }
}
