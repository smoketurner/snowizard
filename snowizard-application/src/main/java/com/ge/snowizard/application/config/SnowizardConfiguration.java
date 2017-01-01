package com.ge.snowizard.application.config;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class SnowizardConfiguration extends Configuration {
    private static final int MAX_ID = 1024;

    @Min(1)
    @Max(MAX_ID)
    private int workerId = 1;

    @Min(1)
    @Max(MAX_ID)
    private int datacenterId = 1;

    private boolean validateUserAgent = false;

    @Valid
    @NotNull
    public final SwaggerBundleConfiguration swagger = new SwaggerBundleConfiguration();

    @JsonProperty("worker_id")
    public int getWorkerId() {
        return workerId;
    }

    @JsonProperty("worker_id")
    public void setWorkerId(final int workerId) {
        this.workerId = workerId;
    }

    @JsonProperty("datacenter_id")
    public int getDatacenterId() {
        return datacenterId;
    }

    @JsonProperty("datacenter_id")
    public void setDatacenterId(final int datacenterId) {
        this.datacenterId = datacenterId;
    }

    @JsonProperty("validate_user_agent")
    public boolean validateUserAgent() {
        return validateUserAgent;
    }

    @JsonProperty("validate_user_agent")
    public void setValidateUserAgent(boolean validateUserAgent) {
        this.validateUserAgent = validateUserAgent;
    }

    @JsonProperty
    public SwaggerBundleConfiguration getSwagger() {
        return swagger;
    }
}
