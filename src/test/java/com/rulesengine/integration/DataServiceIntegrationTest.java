package com.rulesengine.integration;

import com.rulesengine.client.config.ApiKeyAuthConfig;
import com.rulesengine.client.config.GraphQLServiceConfig;
import com.rulesengine.client.config.RestServiceConfig;
import com.rulesengine.service.DataServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating GraphQL and REST client functionality.
 * These tests show how the clients would be used in practice.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataServiceIntegrationTest {

    @Autowired
    private DataServiceClient dataServiceClient;

    @Test
    void testGraphQLServiceConfigCreation() {
        // Arrange
        GraphQLServiceConfig config = new GraphQLServiceConfig();
        config.setEndpoint("https://api.github.com/graphql");
        config.setQuery("query { viewer { login } }");
        config.setAuthConfig(new ApiKeyAuthConfig("Authorization", "Bearer token"));
        config.setTimeoutMs(5000);

        // Act & Assert
        assertEquals("GRAPHQL", config.getServiceType());
        assertEquals("https://api.github.com/graphql", config.getEndpoint());
        assertEquals("query { viewer { login } }", config.getQuery());
        assertNotNull(config.getAuthConfig());
        assertEquals(5000, config.getTimeoutMs());
    }

    @Test
    void testRestServiceConfigCreation() {
        // Arrange
        RestServiceConfig config = new RestServiceConfig();
        config.setEndpoint("https://jsonplaceholder.typicode.com/users");
        config.setMethod(HttpMethod.GET);
        config.setAuthConfig(new ApiKeyAuthConfig("X-API-Key", "test-key"));
        config.setTimeoutMs(10000);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("page", "1");
        queryParams.put("limit", "10");
        config.setQueryParams(queryParams);

        // Act & Assert
        assertEquals("REST", config.getServiceType());
        assertEquals("https://jsonplaceholder.typicode.com/users", config.getEndpoint());
        assertEquals(HttpMethod.GET, config.getMethod());
        assertNotNull(config.getAuthConfig());
        assertEquals(10000, config.getTimeoutMs());
        assertEquals(2, config.getQueryParams().size());
    }

    @Test
    void testDataServiceClientConfiguration() {
        // This test verifies that the DataServiceClient is properly configured
        // and can handle different service types
        assertNotNull(dataServiceClient);
        
        // Test that the client can differentiate between service types
        GraphQLServiceConfig graphqlConfig = new GraphQLServiceConfig();
        graphqlConfig.setEndpoint("https://example.com/graphql");
        assertEquals("GRAPHQL", graphqlConfig.getServiceType());

        RestServiceConfig restConfig = new RestServiceConfig();
        restConfig.setEndpoint("https://example.com/api");
        assertEquals("REST", restConfig.getServiceType());
    }

    @Test
    void testAuthenticationConfigTypes() {
        // Test different authentication types
        ApiKeyAuthConfig apiKeyAuth = new ApiKeyAuthConfig("X-API-Key", "secret");
        assertEquals("API_KEY", apiKeyAuth.getType());
        assertEquals("X-API-Key", apiKeyAuth.getHeaderName());
        assertEquals("secret", apiKeyAuth.getApiKey());

        // Verify auth configs can be applied to service configs
        GraphQLServiceConfig config = new GraphQLServiceConfig();
        config.setAuthConfig(apiKeyAuth);
        assertNotNull(config.getAuthConfig());
        assertEquals("API_KEY", config.getAuthConfig().getType());
    }
}