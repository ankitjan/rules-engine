package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Authentication configuration for data services")
public class AuthConfigDto {

    @Schema(description = "Authentication type", allowableValues = {"NONE", "API_KEY", "BEARER_TOKEN", "BASIC_AUTH", "OAUTH"}, example = "API_KEY")
    @JsonProperty("type")
    private String type;

    @Schema(description = "API key for API_KEY authentication")
    @JsonProperty("apiKey")
    private String apiKey;

    @Schema(description = "API key header name", example = "X-API-Key")
    @JsonProperty("apiKeyHeader")
    private String apiKeyHeader;

    @Schema(description = "Bearer token for BEARER_TOKEN authentication")
    @JsonProperty("bearerToken")
    private String bearerToken;

    @Schema(description = "Username for BASIC_AUTH authentication")
    @JsonProperty("username")
    private String username;

    @Schema(description = "Password for BASIC_AUTH authentication")
    @JsonProperty("password")
    private String password;

    @Schema(description = "OAuth configuration")
    @JsonProperty("oauthConfig")
    private Map<String, String> oauthConfig;

    @Schema(description = "Additional authentication parameters")
    @JsonProperty("additionalParams")
    private Map<String, String> additionalParams;

    // Constructors
    public AuthConfigDto() {}

    public AuthConfigDto(String type) {
        this.type = type;
    }

    // Static factory methods for common auth types
    public static AuthConfigDto none() {
        return new AuthConfigDto("NONE");
    }

    public static AuthConfigDto apiKey(String apiKey, String headerName) {
        AuthConfigDto config = new AuthConfigDto("API_KEY");
        config.setApiKey(apiKey);
        config.setApiKeyHeader(headerName);
        return config;
    }

    public static AuthConfigDto bearerToken(String token) {
        AuthConfigDto config = new AuthConfigDto("BEARER_TOKEN");
        config.setBearerToken(token);
        return config;
    }

    public static AuthConfigDto basicAuth(String username, String password) {
        AuthConfigDto config = new AuthConfigDto("BASIC_AUTH");
        config.setUsername(username);
        config.setPassword(password);
        return config;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

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

    public Map<String, String> getOauthConfig() {
        return oauthConfig;
    }

    public void setOauthConfig(Map<String, String> oauthConfig) {
        this.oauthConfig = oauthConfig;
    }

    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }

    @Override
    public String toString() {
        return "AuthConfigDto{" +
                "type='" + type + '\'' +
                ", apiKeyHeader='" + apiKeyHeader + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}