package com.rulesengine.service;

import com.rulesengine.client.GraphQLClient;
import com.rulesengine.client.RestClient;
import com.rulesengine.client.config.DataServiceConfig;
import com.rulesengine.client.config.GraphQLServiceConfig;
import com.rulesengine.client.config.RestServiceConfig;
import com.rulesengine.exception.DataServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Unified service for accessing external data services (GraphQL and REST).
 * Provides a single interface for field value retrieval from various sources.
 */
@Service
public class DataServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(DataServiceClient.class);
    
    private final GraphQLClient graphQLClient;
    private final RestClient restClient;
    
    @Autowired
    public DataServiceClient(GraphQLClient graphQLClient, RestClient restClient) {
        this.graphQLClient = graphQLClient;
        this.restClient = restClient;
    }
    
    /**
     * Execute data service request based on configuration type.
     */
    public Object executeRequest(DataServiceConfig config, Map<String, Object> parameters) {
        logger.debug("Executing data service request for type: {}", config.getServiceType());
        
        try {
            switch (config.getServiceType()) {
                case "GRAPHQL":
                    return executeGraphQLRequest((GraphQLServiceConfig) config, parameters);
                case "REST":
                    return executeRestRequest((RestServiceConfig) config, parameters);
                default:
                    throw new DataServiceException("Unsupported service type: " + config.getServiceType());
            }
        } catch (Exception e) {
            logger.error("Data service request failed for type: {}", config.getServiceType(), e);
            throw new DataServiceException("Data service request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate data service connection.
     */
    public void validateConnection(DataServiceConfig config) {
        logger.debug("Validating data service connection for type: {}", config.getServiceType());
        
        try {
            switch (config.getServiceType()) {
                case "GRAPHQL":
                    GraphQLServiceConfig graphQLConfig = (GraphQLServiceConfig) config;
                    graphQLClient.validateConnection(graphQLConfig.getEndpoint(), graphQLConfig.getAuthConfig());
                    break;
                case "REST":
                    RestServiceConfig restConfig = (RestServiceConfig) config;
                    restClient.validateConnection(restConfig.getEndpoint(), restConfig.getAuthConfig());
                    break;
                default:
                    throw new DataServiceException("Unsupported service type: " + config.getServiceType());
            }
            
            logger.info("Data service connection validation successful for type: {}", config.getServiceType());
            
        } catch (Exception e) {
            logger.error("Data service connection validation failed for type: {}", config.getServiceType(), e);
            throw new DataServiceException("Data service connection validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute GraphQL request.
     */
    private Object executeGraphQLRequest(GraphQLServiceConfig config, Map<String, Object> parameters) {
        return graphQLClient.executeQuery(config, parameters);
    }
    
    /**
     * Execute REST request.
     */
    private Object executeRestRequest(RestServiceConfig config, Map<String, Object> parameters) {
        // For REST requests, parameters might be used to substitute placeholders in the URL or body
        // This is a simplified implementation - in practice, you might want more sophisticated
        // parameter substitution logic
        return restClient.executeRequest(config);
    }
}