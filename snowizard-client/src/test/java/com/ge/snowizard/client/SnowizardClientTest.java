package com.ge.snowizard.client;

import static org.assertj.core.api.Assertions.assertThat;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import com.ge.snowizard.api.protos.SnowizardProtos.SnowizardResponse;
import com.google.common.base.Optional;
import io.dropwizard.jersey.params.IntParam;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import io.dropwizard.jersey.protobuf.ProtocolBufferMessageBodyProvider;
import io.dropwizard.testing.junit.DropwizardClientRule;

public class SnowizardClientTest {

    @Path("/")
    public static class IdResource {
        @GET
        @Produces(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
        public SnowizardResponse fetch(
                @HeaderParam(HttpHeaders.USER_AGENT) final String agent,
                @QueryParam("count") final Optional<IntParam> count) {
            final SnowizardResponse.Builder builder = SnowizardResponse
                    .newBuilder();
            if (count.isPresent()) {
                for (int i = 1; i <= count.get().get(); i++) {
                    builder.addId(i);
                }
            }
            return builder.build();
        }
    }

    @Path("/ping")
    public static class PingResource {
        @GET
        public String ping() {
            return "pong";
        }
    }

    @Path("/version")
    public static class VersionResource {
        @GET
        public String version() {
            return "1.0.0";
        }
    }

    @ClassRule
    public final static DropwizardClientRule resources = new DropwizardClientRule(
            new ProtocolBufferMessageBodyProvider(), new IdResource(),
            new PingResource(), new VersionResource());

    private static SnowizardClient client;

    @BeforeClass
    public static void setUp() {
        final SnowizardClientBuilder builder = new SnowizardClientBuilder(
                resources.getEnvironment());
        final SnowizardClientConfiguration configuration = new SnowizardClientConfiguration();
        configuration.setUri(resources.baseUri());
        client = builder.build(configuration);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testGetId() throws Exception {
        final long actual = client.getId();
        assertThat(actual).isGreaterThan(0);
    }

    @Test
    public void testPing() throws Exception {
        assertThat(client.ping()).isTrue();
    }

    @Test
    public void testVersion() throws Exception {
        assertThat(client.version()).isEqualTo("1.0.0");
    }
}
