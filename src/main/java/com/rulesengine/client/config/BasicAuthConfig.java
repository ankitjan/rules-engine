package com.rulesengine.client.config;

import org.springframework.http.HttpHeaders;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Basic authentication configuration.
 */
public class BasicAuthConfig extends AuthConfig {
    
    private String username;
    private String password;
    
    public BasicAuthConfig() {}
    
    public BasicAuthConfig(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    @Override
    public String getType() {
        return "BASIC_AUTH";
    }
    
    @Override
    public void applyAuth(HttpHeaders headers) {
        if (username != null && password != null) {
            String credentials = username + ":" + password;
            String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
        }
    }
    
    // Getters and setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}