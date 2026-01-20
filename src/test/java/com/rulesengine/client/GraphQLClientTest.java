package com.rulesengine.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.client.config.ApiKeyAuthConfig;
import com.rulesengine.client.config.BearerTokenAuthConfig;
import com.rulesengine.client.config.GraphQLServiceConfig;
import com.rulesengine.exception.DataServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphQLClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private GraphQLClient graphQLClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        graphQLClient = new GraphQLClient(restTemplate, objectMapper);
    }

    @Test
    void executeQuery_WithValidResponse_ReturnsData() {
        // Arrange
        String endpoint = "https://api.example.com/graphql";
        String query = "{ user { id name } }";
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", "123");

        String responseBody = "{\"data\":{\"user\":{\"id\":\"123\",\"name\":\"John Doe\"}}}";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        Object result = graphQLClient.executeQuery(endpoint, query, variables, null, 30000, null);

        // Assert
        assertNotNull(result);
        verify(restTemplate).exchange(eq(endpoint), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void executeQuery_WithGraphQLErrors_ThrowsException() {
        // Arrange
        String endpoint = "https://api.example.com/graphql";
        String query = "{ user { id name } }";

        String responseBody = "{\"errors\":[{\"message\":\"User not found\"}]}";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act & Assert
        assertThrows(DataServiceException.class, () -> {
            graphQLClient.executeQuery(endpoint, query, null, null, 30000, null);
        });
    }

    @Test
    void executeQuery_WithApiKeyAuth_AppliesAuthentication() {
        // Arrange
        String endpoint = "https://api.example.com/graphql";
        String query = "{ user { id } }";
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig("X-API-Key", "test-key");

        String responseBody = "{\"data\":{\"user\":{\"id\":\"123\"}}}";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        graphQLClient.executeQuery(endpoint, query, null, authConfig, 30000, null);

        // Assert
        verify(restTemplate).exchange(eq(endpoint), eq(HttpMethod.POST), argThat(entity -> {
            HttpHeaders headers = entity.getHeaders();
            return "test-key".equals(headers.getFirst("X-API-Key"));
        }), eq(String.class));
    }

    @Test
    void executeQuery_WithBearerTokenAuth_AppliesAuthentication() {
        // Arrange
        String endpoint = "https://api.example.com/graphql";
        String query = "{ user { id } }";
        BearerTokenAuthConfig authConfig = new BearerTokenAuthConfig("test-token");

        String responseBody = "{\"data\":{\"user\":{\"id\":\"123\"}}}";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        graphQLClient.executeQuery(endpoint, query, null, authConfig, 30000, null);

        // Assert
        verify(restTemplate).exchange(eq(endpoint), eq(HttpMethod.POST), argThat(entity -> {
            HttpHeaders headers = entity.getHeaders();
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            return authHeader != null && authHeader.startsWith("Bearer test-token");
        }), eq(String.class));
    }

    @Test
    void executeQuery_WithServiceConfig_ExecutesSuccessfully() {
        // Arrange
        GraphQLServiceConfig config = new GraphQLServiceConfig();
        config.setEndpoint("https://api.example.com/graphql");
        config.setQuery("{ user { id } }");
        config.setOperationName("GetUser");
        config.setTimeoutMs(30000);

        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", "123");

        String responseBody = "{\"data\":{\"user\":{\"id\":\"123\"}}}";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        Object result = graphQLClient.executeQuery(config, variables);

        // Assert
        assertNotNull(result);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void executeQuery_WithNetworkFailure_RetriesAndFails() {
        // Arrange
        String endpoint = "https://api.example.com/graphql";
        String query = "{ user { id } }";

        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Network error"));

        // Act & Assert
        assertThrows(DataServiceException.class, () -> {
            graphQLClient.executeQuery(endpoint, query, null, null, 30000, null);
        });

        // Verify retry attempts (should be called 3 times)
        verify(restTemplate, times(3)).exchange(eq(endpoint), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void validateConnection_WithValidEndpoint_Succeeds() {
        // Arrange
        String endpoint = "https://api.example.com/graphql";
        String responseBody = "{\"data\":{\"__schema\":{\"queryType\":{\"name\":\"Query\"}}}}";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act & Assert
        assertDoesNotThrow(() -> {
            graphQLClient.validateConnection(endpoint, null);
        });
    }

    @Test
    void validateConnection_WithInvalidEndpoint_ThrowsException() {
        // Arrange
        String endpoint = "https://invalid.example.com/graphql";

        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // Act & Assert
        assertThrows(DataServiceException.class, () -> {
            graphQLClient.validateConnection(endpoint, null);
        });
    }
}