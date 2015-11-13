package com.ge.snowizard.integration;

import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import com.ge.snowizard.application.SnowizardApplication;
import com.ge.snowizard.application.config.SnowizardConfiguration;
import com.ge.snowizard.client.SnowizardClient;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;

public class SnowizardClientIT {

    private static final int COUNT = 1000;
    private SnowizardClient client;

    @ClassRule
    public static final DropwizardAppRule<SnowizardConfiguration> RULE = new DropwizardAppRule<SnowizardConfiguration>(
            SnowizardApplication.class,
            ResourceHelpers.resourceFilePath("test-snowizard.yml"));

    @Before
    public void setUp() throws Exception {
        final URI uri = UriBuilder
                .fromUri("http://localhost:" + RULE.getLocalPort()).build();
        client = new SnowizardClient(uri);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testClientGetId() throws Exception {
        final long startTime = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            client.getId();
        }

        final long delta = (System.nanoTime() - startTime) / 1000000;
        System.out.println(String.format("generated %d (serially) ids in %d ms",
                COUNT, delta));
    }

    @Test
    public void testClientGetIds() throws Exception {
        final long startTime = System.nanoTime();
        client.getIds(COUNT);

        final long delta = (System.nanoTime() - startTime) / 1000000;
        System.out.println(String.format("generated %d (parallel) ids in %d ms",
                COUNT, delta));
    }
}
