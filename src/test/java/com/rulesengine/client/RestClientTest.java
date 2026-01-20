package com.rulesengine.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.client.config.ApiKeyAuthConfig;
import com.rulesengine.client.config.BasicAuthConfig;
import com.rulesengine.client.config.RestServiceConfig;
import com.rulesengine.exception.DataServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        restClient = new RestClient(restTemplate, objectMapper);
    }

    @Test
    void executeRequest_WithGetMethod_ReturnsData() {
        // Arrange
        String endpoint = "https://api.example.com/users/123";
        String responseBody = "{\"id\":\"123\",\"name\":\"John Doe\"}";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        Object result = restClient.executeRequest(endpoint, HttpMethod.GET, null, null, null, null, 30000);

        // Assert
        assertNotNull(result);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void executeRequest_WithPostMethod_SendsRequestBody() {
        // Arrange
        String endpoint = "https://api.example.com/users";
        String requestBody = "{\"name\":\"John Doe\"}";
        String responseBody = "{\"id\":\"123\",\"name\":\"John Doe\"}";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.CREATED);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        Object result = restClient.executeRequest(endpoint, HttpMethod.POST, requestBody, null, null, null, 30000);

        // Assert
        assertNotNull(result);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), argThat(entity -> {
            return requestBody.equals(entity.getBody());
        }), eq(String.class));
    }

    @Test
    void executeRequest_WithQueryParams_BuildsCorrectUri() {
        // Arrange
        String endpoint = "https://api.example.com/users";
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("page", "1");
        queryParams.put("size", "10");

        String responseBody = "[]";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        restClient.executeRequest(endpoint, HttpMethod.GET, null, null, queryParams, null, 30000);

        // Assert
        verify(restTemplate).exchange(argThat(uri -> {
            String uriString = uri.toString();
            return uriString.contains("page=1") && uriString.contains("size=10");
        }), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void executeRequest_WithApiKeyAuth_AppliesAuthentication() {
        // Arrange
        String endpoint = "https://api.example.com/users";
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig("X-API-Key", "test-key");

        String responseBody = "[]";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        restClient.executeRequest(endpoint, HttpMethod.GET, null, null, null, authConfig, 30000);

        // Assert
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.GET), argThat(entity -> {
            HttpHeaders headers = entity.getHeaders();
            return "test-key".equals(headers.getFirst("X-API-Key"));
        }), eq(String.class));
    }

    @Test
    void executeRequest_WithBasicAuth_AppliesAuthentication() {
        // Arrange
        String endpoint = "https://api.example.com/users";
        BasicAuthConfig authConfig = new BasicAuthConfig("user", "pass");

        String responseBody = "[]";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        restClient.executeRequest(endpoint, HttpMethod.GET, null, null, null, authConfig, 30000);

        // Assert
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.GET), argThat(entity -> {
            HttpHeaders headers = entity.getHeaders();
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            return authHeader != null && authHeader.startsWith("Basic ");
        }), eq(String.class));
    }

    @Test
    void executeRequest_WithCustomHeaders_AppliesHeaders() {
        // Arrange
        String endpoint = "https://api.example.com/users";
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Custom-Header", "custom-value");
        customHeaders.put("Accept", "application/json");

        String responseBody = "[]";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        restClient.executeRequest(endpoint, HttpMethod.GET, null, customHeaders, null, null, 30000);

        // Assert
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.GET), argThat(entity -> {
            HttpHeaders headers = entity.getHeaders();
            return "custom-value".equals(headers.getFirst("X-Custom-Header")) &&
                   "application/json".equals(headers.getFirst("Accept"));
        }), eq(String.class));
    }

    @Test
    void executeRequest_WithServiceConfig_ExecutesSuccessfully() {
        // Arrange
        RestServiceConfig config = new RestServiceConfig();
        config.setEndpoint("https://api.example.com/users");
        config.setMethod(HttpMethod.GET);
        config.setTimeoutMs(30000);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("active", "true");
        config.setQueryParams(queryParams);

        String responseBody = "[]";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        Object result = restClient.executeRequest(config);

        // Assert
        assertNotNull(result);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void executeRequest_WithNetworkFailure_RetriesAndFails() {
        // Arrange
        String endpoint = "https://api.example.com/users";

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Network error"));

        // Act & Assert
        assertThrows(DataServiceException.class, () -> {
            restClient.executeRequest(endpoint, HttpMethod.GET, null, null, null, null, 30000);
        });

        // Verify retry attempts (should be called 3 times)
        verify(restTemplate, times(3)).exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void validateConnection_WithValidEndpoint_Succeeds() {
        // Arrange
        String endpoint = "https://api.example.com/health";
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);

        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(response);

        // Act & Assert
        assertDoesNotThrow(() -> {
            restClient.validateConnection(endpoint, null);
        });
    }

    @Test
    void validateConnection_WithInvalidEndpoint_ThrowsException() {
        // Arrange
        String endpoint = "https://invalid.example.com/health";

        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // Act & Assert
        assertThrows(DataServiceException.class, () -> {
            restClient.validateConnection(endpoint, null);
        });
    }

    @Test
    void executeRequest_WithEmptyResponse_ReturnsNull() {
        // Arrange
        String endpoint = "https://api.example.com/users/123";
        ResponseEntity<String> response = new ResponseEntity<>("", HttpStatus.NO_CONTENT);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        // Act
        Object result = restClient.executeRequest(endpoint, HttpMethod.DELETE, null, null, null, null, 30000);

        // Assert
        assertNull(result);
    }
}