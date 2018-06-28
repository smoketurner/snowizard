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
package com.smoketurner.snowizard.application;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.smoketurner.dropwizard.zipkin.ZipkinBundle;
import com.smoketurner.dropwizard.zipkin.ZipkinFactory;
import com.smoketurner.snowizard.application.config.SnowizardConfiguration;
import com.smoketurner.snowizard.application.exceptions.SnowizardExceptionMapper;
import com.smoketurner.snowizard.application.health.EmptyHealthCheck;
import com.smoketurner.snowizard.application.resources.IdResource;
import com.smoketurner.snowizard.application.resources.PingResource;
import com.smoketurner.snowizard.application.resources.VersionResource;
import com.smoketurner.snowizard.core.IdWorker;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.protobuf.ProtobufBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class SnowizardApplication extends Application<SnowizardConfiguration> {

  public static void main(final String[] args) throws Exception {
    new SnowizardApplication().run(args);
  }

  @Override
  public String getName() {
    return "snowizard";
  }

  @Override
  public void initialize(final Bootstrap<SnowizardConfiguration> bootstrap) {
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

    // add Zipkin bundle
    bootstrap.addBundle(
        new ZipkinBundle<SnowizardConfiguration>(getName()) {
          @Override
          public ZipkinFactory getZipkinFactory(final SnowizardConfiguration configuration) {
            return configuration.getZipkin();
          }
        });

    bootstrap.addBundle(new ProtobufBundle());
  }

  @Override
  public void run(final SnowizardConfiguration config, final Environment environment)
      throws Exception {

    // set up zipkin tracing
    config.getZipkin().build(environment);

    environment.jersey().register(SnowizardExceptionMapper.class);

    final IdWorker worker =
        IdWorker.builder(config.getWorkerId(), config.getDatacenterId())
            .withMetricRegistry(environment.metrics())
            .withValidateUserAgent(config.validateUserAgent())
            .build();

    environment
        .metrics()
        .register(
            MetricRegistry.name(SnowizardApplication.class, "worker_id"),
            (Gauge<Integer>) config::getWorkerId);

    environment
        .metrics()
        .register(
            MetricRegistry.name(SnowizardApplication.class, "datacenter_id"),
            (Gauge<Integer>) config::getDatacenterId);

    // health check
    environment.healthChecks().register("empty", new EmptyHealthCheck());

    // resources
    environment.jersey().register(new IdResource(worker));
    environment.jersey().register(new PingResource());
    environment.jersey().register(new VersionResource());
  }
}
