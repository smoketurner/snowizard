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
package com.smoketurner.snowizard.integration;

import com.smoketurner.snowizard.application.SnowizardApplication;
import com.smoketurner.snowizard.application.config.SnowizardConfiguration;
import com.smoketurner.snowizard.client.SnowizardClient;
import com.smoketurner.snowizard.client.SnowizardClientBuilder;
import com.smoketurner.snowizard.client.SnowizardClientConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.util.Duration;
import java.net.URI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class SnowizardClientIT {

  private static final long NANOS_IN_MILLIS = Duration.milliseconds(1).toNanoseconds();
  private static final int COUNT = 1000;
  private static SnowizardClient client;

  @ClassRule
  public static final DropwizardAppRule<SnowizardConfiguration> RULE =
      new DropwizardAppRule<SnowizardConfiguration>(
          SnowizardApplication.class, ResourceHelpers.resourceFilePath("test-snowizard.yml"));

  @BeforeClass
  public static void setUp() {
    final SnowizardClientBuilder builder = new SnowizardClientBuilder(RULE.getEnvironment());
    final SnowizardClientConfiguration configuration = new SnowizardClientConfiguration();
    configuration.setUri(URI.create("http://127.0.0.1:" + RULE.getLocalPort()));
    configuration.setTimeout(Duration.seconds(1));
    configuration.setRetries(3);
    configuration.setGzipEnabled(true);
    configuration.setKeepAlive(Duration.milliseconds(500));
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
    System.out.println(String.format("generated %d (serially) ids in %d ms", COUNT, delta));
  }

  @Test
  public void testClientGetIds() throws Exception {
    final long startTime = System.nanoTime();
    client.getIds(COUNT);

    final long delta = (System.nanoTime() - startTime) / NANOS_IN_MILLIS;
    System.out.println(String.format("generated %d (parallel) ids in %d ms", COUNT, delta));
  }
}
