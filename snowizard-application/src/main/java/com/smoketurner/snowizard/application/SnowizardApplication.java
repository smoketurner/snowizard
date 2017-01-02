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
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

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
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)));

        // add Swagger bundle
        bootstrap.addBundle(new SwaggerBundle<SnowizardConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    final SnowizardConfiguration configuration) {
                return configuration.getSwagger();
            }
        });

        // add Zipkin bundle
        bootstrap
                .addBundle(new ZipkinBundle<SnowizardConfiguration>(getName()) {
                    @Override
                    public ZipkinFactory getZipkinFactory(
                            final SnowizardConfiguration configuration) {
                        return configuration.getZipkin();
                    }
                });

        bootstrap.addBundle(new ProtobufBundle());
    }

    @Override
    public void run(final SnowizardConfiguration config,
            final Environment environment) throws Exception {

        // set up zipkin tracing
        config.getZipkin().build(environment);

        environment.jersey().register(SnowizardExceptionMapper.class);

        final IdWorker worker = IdWorker
                .builder(config.getWorkerId(), config.getDatacenterId())
                .withMetricRegistry(environment.metrics())
                .withValidateUserAgent(config.validateUserAgent()).build();

        environment.metrics().register(
                MetricRegistry.name(SnowizardApplication.class, "worker_id"),
                (Gauge<Integer>) config::getWorkerId);

        environment.metrics().register(
                MetricRegistry.name(SnowizardApplication.class,
                        "datacenter_id"),
                (Gauge<Integer>) config::getDatacenterId);

        // health check
        environment.healthChecks().register("empty", new EmptyHealthCheck());

        // resources
        environment.jersey().register(new IdResource(worker));
        environment.jersey().register(new PingResource());
        environment.jersey().register(new VersionResource());
    }
}
