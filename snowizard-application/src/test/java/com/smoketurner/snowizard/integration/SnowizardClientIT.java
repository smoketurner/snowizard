package com.smoketurner.snowizard.integration;

import java.net.URI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import com.smoketurner.snowizard.application.SnowizardApplication;
import com.smoketurner.snowizard.application.config.SnowizardConfiguration;
import com.smoketurner.snowizard.client.SnowizardClient;
import com.smoketurner.snowizard.client.SnowizardClientBuilder;
import com.smoketurner.snowizard.client.SnowizardClientConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.util.Duration;

public class SnowizardClientIT {

    private static final long NANOS_IN_MILLIS = Duration.milliseconds(1)
            .toNanoseconds();
    private static final int COUNT = 1000;
    private static SnowizardClient client;

    @ClassRule
    public static final DropwizardAppRule<SnowizardConfiguration> RULE = new DropwizardAppRule<SnowizardConfiguration>(
            SnowizardApplication.class,
            ResourceHelpers.resourceFilePath("test-snowizard.yml"));

    @BeforeClass
    public static void setUp() {
        final SnowizardClientBuilder builder = new SnowizardClientBuilder(
                RULE.getEnvironment());
        final SnowizardClientConfiguration configuration = new SnowizardClientConfiguration();
        configuration
                .setUri(URI.create("http://127.0.0.1:" + RULE.getLocalPort()));
        client = builder.build(configuration);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testClientGetId() throws Exception {
        final long startTime = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            client.getId();
        }

        final long delta = (System.nanoTime() - startTime) / NANOS_IN_MILLIS;
        System.out.println(String.format("generated %d (serially) ids in %d ms",
                COUNT, delta));
    }

    @Test
    public void testClientGetIds() throws Exception {
        final long startTime = System.nanoTime();
        client.getIds(COUNT);

        final long delta = (System.nanoTime() - startTime) / NANOS_IN_MILLIS;
        System.out.println(String.format("generated %d (parallel) ids in %d ms",
                COUNT, delta));
    }
}
