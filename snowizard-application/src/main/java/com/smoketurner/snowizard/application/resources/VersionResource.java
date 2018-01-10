package com.smoketurner.snowizard.application.resources;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.jersey.caching.CacheControl;

@Path("/version")
public class VersionResource {

    private final String version;

    /**
     * Constructor
     */
    public VersionResource() {
        version = getClass().getPackage().getImplementationVersion();
    }

    /**
     * Constructor
     *
     * @param version
     *            Version to expose in the endpoint
     */
    @VisibleForTesting
    public VersionResource(@Nonnull final String version) {
        this.version = Objects.requireNonNull(version);
    }

    @GET
    @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
    public Response getVersion() {
        return Response.ok(version).type(MediaType.TEXT_PLAIN).build();
    }
}
