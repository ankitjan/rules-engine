package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@Schema(description = "Data service configuration for external data retrieval")
public class DataServiceConfigDto {

    @NotBlank(message = "Service type is required")
    @Schema(description = "Type of data service", allowableValues = {"GRAPHQL", "REST"}, example = "GRAPHQL")
    @JsonProperty("serviceType")
    private String serviceType;

    @NotBlank(message = "Endpoint is required")
    @Schema(description = "Service endpoint URL", example = "https://api.example.com/graphql")
    @JsonProperty("endpoint")
    private String endpoint;

    @Schema(description = "GraphQL query or REST path template", example = "query getCustomer($id: ID!) { customer(id: $id) { id name email } }")
    @JsonProperty("query")
    private String query;

    @Schema(description = "HTTP headers for the request")
    @JsonProperty("headers")
    private Map<String, String> headers;

    @Schema(description = "Authentication configuration")
    @JsonProperty("authentication")
    private AuthConfigDto authentication;

    @Schema(description = "Request timeout in milliseconds", example = "5000")
    @JsonProperty("timeoutMs")
    private Integer timeoutMs;

    @Schema(description = "Number of retry attempts on failure", example = "3")
    @JsonProperty("retryAttempts")
    private Integer retryAttempts;

    @Schema(description = "Field names this service depends on")
    @JsonProperty("dependsOn")
    private List<String> dependsOn;

    @Schema(description = "Whether to cache responses", example = "true")
    @JsonProperty("cacheable")
    private Boolean cacheable;

    @Schema(description = "Cache TTL in seconds", example = "300")
    @JsonProperty("cacheTtlSeconds")
    private Integer cacheTtlSeconds;

    // Constructors
    public DataServiceConfigDto() {}

    public DataServiceConfigDto(String serviceType, String endpoint, String query) {
        this.serviceType = serviceType;
        this.endpoint = endpoint;
        this.query = query;
    }

    // Getters and Setters
    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public AuthConfigDto getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AuthConfigDto authentication) {
        this.authentication = authentication;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(Integer retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public Boolean getCacheable() {
        return cacheable;
    }

    public void setCacheable(Boolean cacheable) {
        this.cacheable = cacheable;
    }

    public Integer getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(Integer cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    @Override
    public String toString() {
        return "DataServiceConfigDto{" +
                "serviceType='" + serviceType + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", timeoutMs=" + timeoutMs +
                ", retryAttempts=" + retryAttempts +
                ", cacheable=" + cacheable +
                '}';
    }
}