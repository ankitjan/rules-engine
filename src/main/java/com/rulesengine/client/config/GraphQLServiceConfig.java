package com.rulesengine.client.config;

/**
 * Configuration for GraphQL data services.
 */
public class GraphQLServiceConfig extends DataServiceConfig {
    
    private String query;
    private String operationName;
    
    public GraphQLServiceConfig() {}
    
    public GraphQLServiceConfig(String endpoint, String query) {
        this.endpoint = endpoint;
        this.query = query;
    }
    
    @Override
    public String getServiceType() {
        return "GRAPHQL";
    }
    
    // Getters and setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getOperationName() {
        return operationName;
    }
    
    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}