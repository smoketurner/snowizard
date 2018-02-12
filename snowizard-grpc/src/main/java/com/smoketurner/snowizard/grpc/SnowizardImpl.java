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

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.Empty;
import com.smoketurner.snowizard.core.IdWorker;
import com.smoketurner.snowizard.exceptions.InvalidSystemClock;
import com.smoketurner.snowizard.grpc.protos.SnowizardGrpc;
import com.smoketurner.snowizard.grpc.protos.SnowizardRequest;
import com.smoketurner.snowizard.grpc.protos.SnowizardResponse;
import io.grpc.stub.StreamObserver;

public class SnowizardImpl extends SnowizardGrpc.SnowizardImplBase {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SnowizardImpl.class);
    private static final int BATCH_SIZE = 1000;
    private final IdWorker worker;

    /**
     * Constructor
     *
     * @param worker
     *            ID generator
     */
    public SnowizardImpl(final IdWorker worker) {
        this.worker = Objects.requireNonNull(worker);
    }

    @Override
    public void getId(Empty request,
            StreamObserver<SnowizardResponse> responseObserver) {

        final long startTime = System.nanoTime();
        LOGGER.info("Requested to generate 1 id");

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
    public void getIds(SnowizardRequest request,
            StreamObserver<SnowizardResponse> responseObserver) {

        final long startTime = System.nanoTime();
        final int count = request.getCount();
        final int fullBatches = count / BATCH_SIZE;
        final int remaining = count - (fullBatches * BATCH_SIZE);

        LOGGER.debug(
                "Requested to generate {} ids ({} batches of {}, 1 batch of {})",
                count, fullBatches, BATCH_SIZE, remaining);

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

        LOGGER.debug("Generated {} ids in {}ns", generated,
                (System.nanoTime() - startTime));
    }

    private SnowizardResponse generateIds(final int count)
            throws InvalidSystemClock {

        final SnowizardResponse.Builder builder = SnowizardResponse
                .newBuilder();
        for (int i = 0; i < count; i++) {
            builder.addId(worker.nextId());
        }
        return builder.build();
    }
}
