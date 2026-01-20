package com.rulesengine.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.client.config.AuthConfig;
import com.rulesengine.client.config.RestServiceConfig;
import com.rulesengine.exception.DataServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Client for executing REST API calls against external services.
 * Supports authentication, connection pooling, and timeout management.
 */
@Component
public class RestClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RestClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Execute REST API call using service configuration.
     */
    public Object executeRequest(RestServiceConfig config) {
        return executeRequest(
            config.getEndpoint(),
            config.getMethod(),
            config.getRequestBody(),
            config.getHeaders(),
            config.getQueryParams(),
            config.getAuthConfig(),
            config.getTimeoutMs()
        );
    }
    
    /**
     * Execute REST API call with specified parameters.
     */
    public Object executeRequest(String endpoint, HttpMethod method, String requestBody,
                               Map<String, String> headers, Map<String, String> queryParams,
                               AuthConfig authConfig, int timeoutMs) {
        
        logger.debug("Executing {} request to endpoint: {}", method, endpoint);
        
        try {
            // Build URI with query parameters
            URI uri = buildUri(endpoint, queryParams);
            
            // Build HTTP headers
            HttpHeaders httpHeaders = buildHeaders(headers, authConfig);
            
            // Create HTTP entity
            HttpEntity<String> entity = new HttpEntity<>(requestBody, httpHeaders);
            
            // Execute request with retry logic
            return executeWithRetry(uri, method, entity, 3);
            
        } catch (Exception e) {
            logger.error("Failed to execute REST request to endpoint: {}", endpoint, e);
            throw new DataServiceException("REST request execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute REST API call with simplified parameters for contract testing.
     */
    public Object executeRequest(String method, String endpoint, String requestBody, AuthConfig authConfig) {
        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
        return executeRequest(endpoint, httpMethod, requestBody, null, null, authConfig, 30000);
    }
    
    /**
     * Validate connection to REST endpoint.
     */
    public void validateConnection(String endpoint, AuthConfig authConfig) {
        logger.debug("Validating REST connection to endpoint: {}", endpoint);
        
        try {
            // Use HEAD request for connection validation
            HttpHeaders headers = new HttpHeaders();
            if (authConfig != null) {
                authConfig.applyAuth(headers);
            }
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                endpoint,
                HttpMethod.HEAD,
                entity,
                Void.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("REST connection validation successful for endpoint: {}", endpoint);
            } else {
                throw new DataServiceException("REST endpoint returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("REST connection validation failed for endpoint: {}", endpoint, e);
            throw new DataServiceException("REST connection validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build URI with query parameters.
     */
    private URI buildUri(String endpoint, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
        
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        
        return builder.build().toUri();
    }
    
    /**
     * Build HTTP headers with authentication.
     */
    private HttpHeaders buildHeaders(Map<String, String> customHeaders, AuthConfig authConfig) {
        HttpHeaders headers = new HttpHeaders();
        
        // Add custom headers
        if (customHeaders != null) {
            customHeaders.forEach(headers::set);
        }
        
        // Apply authentication
        if (authConfig != null) {
            authConfig.applyAuth(headers);
        }
        
        // Set default content type if not specified
        if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        
        return headers;
    }
    
    /**
     * Execute HTTP request with retry logic.
     */
    private Object executeWithRetry(URI uri, HttpMethod method, HttpEntity<String> entity, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("REST request attempt {} of {}", attempt, maxRetries);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    method,
                    entity,
                    String.class
                );
                
                return parseRestResponse(response);
                
            } catch (RestClientException e) {
                lastException = e;
                logger.warn("REST request attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        // Exponential backoff
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new DataServiceException("Request interrupted", ie);
                    }
                }
            }
        }
        
        throw new DataServiceException("REST request failed after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Parse REST response.
     */
    private Object parseRestResponse(ResponseEntity<String> response) {
        try {
            String responseBody = response.getBody();
            
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return null;
            }
            
            // Try to parse as JSON
            return objectMapper.readValue(responseBody, Object.class);
            
        } catch (Exception e) {
            logger.error("Failed to parse REST response: {}", response.getBody(), e);
            throw new DataServiceException("Failed to parse REST response: " + e.getMessage(), e);
        }
    }
}