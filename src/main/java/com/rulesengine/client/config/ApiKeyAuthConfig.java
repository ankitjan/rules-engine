package com.rulesengine.client.config;

import org.springframework.http.HttpHeaders;

/**
 * API Key authentication configuration.
 */
public class ApiKeyAuthConfig extends AuthConfig {
    
    private String headerName;
    private String apiKey;
    
    public ApiKeyAuthConfig() {}
    
    public ApiKeyAuthConfig(String headerName, String apiKey) {
        this.headerName = headerName;
        this.apiKey = apiKey;
    }
    
    @Override
    public String getType() {
        return "API_KEY";
    }
    
    @Override
    public void applyAuth(HttpHeaders headers) {
        if (headerName != null && apiKey != null) {
            headers.set(headerName, apiKey);
        }
    }
    
    // Getters and setters
    public String getHeaderName() {
        return headerName;
    }
    
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}