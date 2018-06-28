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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.smoketurner.snowizard.application.config.SnowizardConfiguration;
import com.smoketurner.snowizard.application.resources.IdResource;
import com.smoketurner.snowizard.application.resources.PingResource;
import com.smoketurner.snowizard.application.resources.VersionResource;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class SnowizardApplicationTest {
  private final String AGENT = "snowizard-client";
  private final Environment environment = mock(Environment.class);
  private final JerseyEnvironment jersey = mock(JerseyEnvironment.class);
  private final LifecycleEnvironment lifecycle = mock(LifecycleEnvironment.class);
  private final MetricRegistry metrics = mock(MetricRegistry.class);
  private final HealthCheckRegistry healthChecks = mock(HealthCheckRegistry.class);
  private final SnowizardApplication application = new SnowizardApplication();
  private final SnowizardConfiguration config = new SnowizardConfiguration();

  @ClassRule
  public static final DropwizardAppRule<SnowizardConfiguration> RULE =
      new DropwizardAppRule<SnowizardConfiguration>(
          SnowizardApplication.class, ResourceHelpers.resourceFilePath("test-snowizard.yml"));

  @Before
  public void setUp() {
    config.getZipkin().setServiceName("snowizard");
    when(environment.jersey()).thenReturn(jersey);
    when(environment.lifecycle()).thenReturn(lifecycle);
    when(environment.metrics()).thenReturn(metrics);
    when(environment.healthChecks()).thenReturn(healthChecks);
  }

  @Test
  public void buildsAIdResource() throws Exception {
    application.run(config, environment);
    verify(jersey).register(isA(IdResource.class));
  }

  @Test
  public void buildsAPingResource() throws Exception {
    application.run(config, environment);
    verify(jersey).register(isA(PingResource.class));
  }

  @Test
  public void buildsAVersionResource() throws Exception {
    application.run(config, environment);
    verify(jersey).register(isA(VersionResource.class));
  }

  @Test
  public void testCanGetIdOverHttp() throws Exception {
    final String response =
        new JerseyClientBuilder(RULE.getEnvironment())
            .build("")
            .target("http://127.0.0.1:" + RULE.getLocalPort())
            .request(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.USER_AGENT, AGENT)
            .get(String.class);
    final long id = Long.valueOf(response);
    assertThat(id).isNotNull();
  }
}
