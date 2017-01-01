package com.smoketurner.snowizard.client;

import java.net.URI;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import io.dropwizard.client.JerseyClientConfiguration;

public class SnowizardClientConfiguration extends JerseyClientConfiguration {

    @NotNull
    private URI uri = URI.create("http://127.0.0.1:8080");

    public URI getUri() {
        return uri;
    }

    public void setUri(@Nonnull final URI uri) {
        this.uri = uri;
    }
}
