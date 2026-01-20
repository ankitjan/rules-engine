package com.rulesengine.client.config;

import org.springframework.http.HttpHeaders;

/**
 * OAuth authentication configuration.
 * For simplicity, this implementation assumes the OAuth token is already obtained.
 */
public class OAuthConfig extends AuthConfig {
    
    private String accessToken;
    private String tokenType = "Bearer"; // Default to Bearer
    
    public OAuthConfig() {}
    
    public OAuthConfig(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public OAuthConfig(String accessToken, String tokenType) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }
    
    @Override
    public String getType() {
        return "OAUTH";
    }
    
    @Override
    public void applyAuth(HttpHeaders headers) {
        if (accessToken != null) {
            headers.set(HttpHeaders.AUTHORIZATION, tokenType + " " + accessToken);
        }
    }
    
    // Getters and setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}