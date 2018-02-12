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

import org.slf4j.bridge.SLF4JBridgeHandler;
import io.airlift.airline.Cli;
import io.airlift.airline.Cli.CliBuilder;
import io.airlift.airline.Help;

public class Runner {

    public static void main(String[] args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        cli().parse(args).run();
    }

    private static Cli<Runnable> cli() {
        final CliBuilder<Runnable> builder = Cli.builder("snowizard");

        builder.withDescription("Launch the snowizard service")
                .withDefaultCommand(Help.class).withCommand(Help.class)
                .withCommand(SnowizardClient.Cmd.class)
                .withCommand(SnowizardServer.Cmd.class);

        return builder.build();
    }
}
