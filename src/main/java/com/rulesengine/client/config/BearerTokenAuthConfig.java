package com.rulesengine.client.config;

import org.springframework.http.HttpHeaders;

/**
 * Bearer Token (JWT) authentication configuration.
 */
public class BearerTokenAuthConfig extends AuthConfig {
    
    private String token;
    
    public BearerTokenAuthConfig() {}
    
    public BearerTokenAuthConfig(String token) {
        this.token = token;
    }
    
    @Override
    public String getType() {
        return "BEARER_TOKEN";
    }
    
    @Override
    public void applyAuth(HttpHeaders headers) {
        if (token != null) {
            headers.setBearerAuth(token);
        }
    }
    
    // Getters and setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}