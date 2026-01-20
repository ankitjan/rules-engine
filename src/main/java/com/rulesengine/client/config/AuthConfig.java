package com.rulesengine.client.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base authentication configuration for external service clients.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ApiKeyAuthConfig.class, name = "API_KEY"),
    @JsonSubTypes.Type(value = BearerTokenAuthConfig.class, name = "BEARER_TOKEN"),
    @JsonSubTypes.Type(value = BasicAuthConfig.class, name = "BASIC_AUTH"),
    @JsonSubTypes.Type(value = OAuthConfig.class, name = "OAUTH")
})
public abstract class AuthConfig {
    
    public abstract String getType();
    
    /**
     * Apply authentication to HTTP headers.
     */
    public abstract void applyAuth(org.springframework.http.HttpHeaders headers);
}