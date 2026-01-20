package com.rulesengine.client.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base configuration for external data services.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "serviceType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = GraphQLServiceConfig.class, name = "GRAPHQL"),
    @JsonSubTypes.Type(value = RestServiceConfig.class, name = "REST")
})
public abstract class DataServiceConfig {
    
    protected String endpoint;
    protected AuthConfig authConfig;
    protected int timeoutMs = 30000; // Default 30 seconds
    protected int maxRetries = 3;
    
    public abstract String getServiceType();
    
    // Getters and setters
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public AuthConfig getAuthConfig() {
        return authConfig;
    }
    
    public void setAuthConfig(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }
    
    public int getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}