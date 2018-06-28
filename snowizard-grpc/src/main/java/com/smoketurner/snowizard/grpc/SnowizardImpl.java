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

import com.google.protobuf.Empty;
import com.smoketurner.snowizard.core.IdWorker;
import com.smoketurner.snowizard.exceptions.InvalidSystemClock;
import com.smoketurner.snowizard.grpc.protos.SnowizardGrpc;
import com.smoketurner.snowizard.grpc.protos.SnowizardRequest;
import com.smoketurner.snowizard.grpc.protos.SnowizardResponse;
import io.grpc.stub.StreamObserver;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnowizardImpl extends SnowizardGrpc.SnowizardImplBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(SnowizardImpl.class);
  private static final int BATCH_SIZE = 1000;
  private final IdWorker worker;

  /**
   * Constructor
   *
   * @param worker ID generator
   */
  public SnowizardImpl(final IdWorker worker) {
    this.worker = Objects.requireNonNull(worker);
  }

  @Override
  public void getId(Empty request, StreamObserver<SnowizardResponse> responseObserver) {

    final long startTime = System.nanoTime();
    LOGGER.debug("Requested to generate 1 id");

    final SnowizardResponse response;
    try {
      response = generateIds(1);
    } catch (InvalidSystemClock e) {
      LOGGER.error("Invalid system clock", e);
      responseObserver.onError(e);
      return;
    }

    responseObserver.onNext(response);
    responseObserver.onCompleted();

    LOGGER.debug("Generated 1 id in {}ns", (System.nanoTime() - startTime));
  }

  @Override
  public void getIds(SnowizardRequest request, StreamObserver<SnowizardResponse> responseObserver) {

    final long startTime = System.nanoTime();
    final int count = request.getCount();
    final int fullBatches = count / BATCH_SIZE;
    final int remaining = count - (fullBatches * BATCH_SIZE);

    LOGGER.debug(
        "Requested to generate {} ids ({} batches of {}, 1 batch of {})",
        count,
        fullBatches,
        BATCH_SIZE,
        remaining);

    int generated = 0;
    SnowizardResponse response;

    if (fullBatches > 0) {

      for (int b = 0; b < fullBatches; b++) {

        try {
          response = generateIds(BATCH_SIZE);
        } catch (InvalidSystemClock e) {
          LOGGER.error("Invalid system clock", e);
          responseObserver.onError(e);
          return;
        }

        generated += response.getIdCount();
        responseObserver.onNext(response);
      }
    }

    if (remaining > 0) {
      try {
        response = generateIds(remaining);
      } catch (InvalidSystemClock e) {
        LOGGER.error("Invalid system clock", e);
        responseObserver.onError(e);
        return;
      }

      generated += response.getIdCount();
      responseObserver.onNext(response);
    }

    responseObserver.onCompleted();

    LOGGER.debug("Generated {} ids in {}ns", generated, (System.nanoTime() - startTime));
  }

  private SnowizardResponse generateIds(final int count) throws InvalidSystemClock {

    final SnowizardResponse.Builder builder = SnowizardResponse.newBuilder();
    for (int i = 0; i < count; i++) {
      builder.addId(worker.nextId());
    }
    return builder.build();
  }
}
