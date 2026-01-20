package com.rulesengine.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.client.config.AuthConfig;
import com.rulesengine.client.config.GraphQLServiceConfig;
import com.rulesengine.exception.DataServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for executing GraphQL queries against external services.
 * Supports authentication, connection pooling, and timeout management.
 */
@Component
public class GraphQLClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphQLClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public GraphQLClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Execute a GraphQL query with variables.
     */
    public Object executeQuery(GraphQLServiceConfig config, Map<String, Object> variables) {
        return executeQuery(config.getEndpoint(), config.getQuery(), variables, 
                          config.getAuthConfig(), config.getTimeoutMs(), config.getOperationName());
    }
    
    /**
     * Execute a GraphQL query against the specified endpoint.
     */
    public Object executeQuery(String endpoint, String query, Map<String, Object> variables, 
                             AuthConfig authConfig, int timeoutMs, String operationName) {
        
        logger.debug("Executing GraphQL query against endpoint: {}", endpoint);
        
        try {
            // Build GraphQL request payload
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("query", query);
            
            if (variables != null && !variables.isEmpty()) {
                requestPayload.put("variables", variables);
            }
            
            if (operationName != null) {
                requestPayload.put("operationName", operationName);
            }
            
            // Build HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Apply authentication
            if (authConfig != null) {
                authConfig.applyAuth(headers);
            }
            
            // Create HTTP entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);
            
            // Execute request with retry logic
            return executeWithRetry(endpoint, entity, timeoutMs, 3);
            
        } catch (Exception e) {
            logger.error("Failed to execute GraphQL query against endpoint: {}", endpoint, e);
            throw new DataServiceException("GraphQL query execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute a GraphQL query with custom retry configuration.
     */
    public Object executeQuery(String endpoint, String query, Map<String, Object> variables, AuthConfig authConfig) {
        return executeQuery(endpoint, query, variables, authConfig, 30000, null);
    }
    
    /**
     * Execute a GraphQL query with retry logic for resilience testing.
     */
    public Object executeQueryWithRetry(String endpoint, String query, Map<String, Object> variables, 
                                       AuthConfig authConfig, int maxRetries) {
        logger.debug("Executing GraphQL query with custom retry logic: {} retries", maxRetries);
        
        try {
            // Build GraphQL request payload
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("query", query);
            
            if (variables != null && !variables.isEmpty()) {
                requestPayload.put("variables", variables);
            }
            
            // Build HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Apply authentication
            if (authConfig != null) {
                authConfig.applyAuth(headers);
            }
            
            // Create HTTP entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);
            
            // Execute request with custom retry logic
            return executeWithRetry(endpoint, entity, 30000, maxRetries);
            
        } catch (Exception e) {
            logger.error("Failed to execute GraphQL query with retry against endpoint: {}", endpoint, e);
            throw new DataServiceException("GraphQL query execution with retry failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate connection to GraphQL endpoint.
     */
    public void validateConnection(String endpoint, AuthConfig authConfig) {
        logger.debug("Validating GraphQL connection to endpoint: {}", endpoint);
        
        try {
            // Use introspection query to validate connection
            String introspectionQuery = "{ __schema { queryType { name } } }";
            executeQuery(endpoint, introspectionQuery, null, authConfig, 10000, null);
            
            logger.info("GraphQL connection validation successful for endpoint: {}", endpoint);
            
        } catch (Exception e) {
            logger.error("GraphQL connection validation failed for endpoint: {}", endpoint, e);
            throw new DataServiceException("GraphQL connection validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute HTTP request with retry logic.
     */
    private Object executeWithRetry(String endpoint, HttpEntity<Map<String, Object>> entity, 
                                   int timeoutMs, int maxRetries) {
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("GraphQL request attempt {} of {}", attempt, maxRetries);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, 
                    HttpMethod.POST, 
                    entity, 
                    String.class
                );
                
                return parseGraphQLResponse(response.getBody());
                
            } catch (RestClientException e) {
                lastException = e;
                logger.warn("GraphQL request attempt {} failed: {}", attempt, e.getMessage());
                
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
        
        throw new DataServiceException("GraphQL request failed after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Parse GraphQL response and extract data or handle errors.
     */
    private Object parseGraphQLResponse(String responseBody) {
        try {
            JsonNode responseNode = objectMapper.readTree(responseBody);
            
            // Check for GraphQL errors
            if (responseNode.has("errors")) {
                JsonNode errorsNode = responseNode.get("errors");
                logger.error("GraphQL response contains errors: {}", errorsNode);
                throw new DataServiceException("GraphQL query returned errors: " + errorsNode.toString());
            }
            
            // Extract data
            if (responseNode.has("data")) {
                JsonNode dataNode = responseNode.get("data");
                return objectMapper.convertValue(dataNode, Object.class);
            }
            
            throw new DataServiceException("GraphQL response missing data field");
            
        } catch (Exception e) {
            logger.error("Failed to parse GraphQL response: {}", responseBody, e);
            throw new DataServiceException("Failed to parse GraphQL response: " + e.getMessage(), e);
        }
    }
}