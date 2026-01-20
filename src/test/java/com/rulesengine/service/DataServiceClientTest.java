package com.rulesengine.service;

import com.rulesengine.client.GraphQLClient;
import com.rulesengine.client.RestClient;
import com.rulesengine.client.config.GraphQLServiceConfig;
import com.rulesengine.client.config.RestServiceConfig;
import com.rulesengine.exception.DataServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataServiceClientTest {

    @Mock
    private GraphQLClient graphQLClient;

    @Mock
    private RestClient restClient;

    private DataServiceClient dataServiceClient;

    @BeforeEach
    void setUp() {
        dataServiceClient = new DataServiceClient(graphQLClient, restClient);
    }

    @Test
    void executeRequest_WithGraphQLConfig_CallsGraphQLClient() {
        // Arrange
        GraphQLServiceConfig config = new GraphQLServiceConfig();
        config.setEndpoint("https://api.example.com/graphql");
        config.setQuery("{ user { id } }");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", "123");

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("user", Map.of("id", "123"));

        when(graphQLClient.executeQuery(eq(config), eq(parameters))).thenReturn(expectedResult);

        // Act
        Object result = dataServiceClient.executeRequest(config, parameters);

        // Assert
        assertEquals(expectedResult, result);
        verify(graphQLClient).executeQuery(eq(config), eq(parameters));
        verifyNoInteractions(restClient);
    }

    @Test
    void executeRequest_WithRestConfig_CallsRestClient() {
        // Arrange
        RestServiceConfig config = new RestServiceConfig();
        config.setEndpoint("https://api.example.com/users/123");
        config.setMethod(HttpMethod.GET);

        Map<String, Object> parameters = new HashMap<>();

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("id", "123");
        expectedResult.put("name", "John Doe");

        when(restClient.executeRequest(eq(config))).thenReturn(expectedResult);

        // Act
        Object result = dataServiceClient.executeRequest(config, parameters);

        // Assert
        assertEquals(expectedResult, result);
        verify(restClient).executeRequest(eq(config));
        verifyNoInteractions(graphQLClient);
    }

    @Test
    void executeRequest_WithUnsupportedServiceType_ThrowsException() {
        // Arrange
        // Create a mock config with unsupported type
        var config = new RestServiceConfig() {
            @Override
            public String getServiceType() {
                return "UNSUPPORTED";
            }
        };

        Map<String, Object> parameters = new HashMap<>();

        // Act & Assert
        assertThrows(DataServiceException.class, () -> {
            dataServiceClient.executeRequest(config, parameters);
        });

        verifyNoInteractions(graphQLClient);
        verifyNoInteractions(restClient);
    }

    @Test
    void validateConnection_WithGraphQLConfig_CallsGraphQLClient() {
        // Arrange
        GraphQLServiceConfig config = new GraphQLServiceConfig();
        config.setEndpoint("https://api.example.com/graphql");

        // Act
        assertDoesNotThrow(() -> {
            dataServiceClient.validateConnection(config);
        });

        // Assert
        verify(graphQLClient).validateConnection(eq(config.getEndpoint()), eq(config.getAuthConfig()));
        verifyNoInteractions(restClient);
    }

    @Test
    void validateConnection_WithRestConfig_CallsRestClient() {
        // Arrange
        RestServiceConfig config = new RestServiceConfig();
        config.setEndpoint("https://api.example.com/health");

        // Act
        assertDoesNotThrow(() -> {
            dataServiceClient.validateConnection(config);
        });

        // Assert
        verify(restClient).validateConnection(eq(config.getEndpoint()), eq(config.getAuthConfig()));
        verifyNoInteractions(graphQLClient);
    }

    @Test
    void validateConnection_WithUnsupportedServiceType_ThrowsException() {
        // Arrange
        var config = new RestServiceConfig() {
            @Override
            public String getServiceType() {
                return "UNSUPPORTED";
            }
        };

        // Act & Assert
        assertThrows(DataServiceException.class, () -> {
            dataServiceClient.validateConnection(config);
        });

        verifyNoInteractions(graphQLClient);
        verifyNoInteractions(restClient);
    }

    @Test
    void executeRequest_WhenGraphQLClientThrows_PropagatesException() {
        // Arrange
        GraphQLServiceConfig config = new GraphQLServiceConfig();
        config.setEndpoint("https://api.example.com/graphql");
        config.setQuery("{ user { id } }");

        Map<String, Object> parameters = new HashMap<>();

        when(graphQLClient.executeQuery(any(), any())).thenThrow(new DataServiceException("GraphQL error"));

        // Act & Assert
        assertThrows(DataServiceException.class, () -> {
            dataServiceClient.executeRequest(config, parameters);
        });
    }

    @Test
    void executeRequest_WhenRestClientThrows_PropagatesException() {
        // Arrange
        RestServiceConfig config = new RestServiceConfig();
        config.setEndpoint("https://api.example.com/users");
        config.setMethod(HttpMethod.GET);

        Map<String, Object> parameters = new HashMap<>();

        when(restClient.executeRequest(any())).thenThrow(new DataServiceException("REST error"));

        // Act & Assert
        assertThrows(DataServiceException.class, () -> {
            dataServiceClient.executeRequest(config, parameters);
        });
    }
}