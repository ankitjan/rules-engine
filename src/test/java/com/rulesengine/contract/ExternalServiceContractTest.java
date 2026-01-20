package com.rulesengine.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.rulesengine.client.GraphQLClient;
import com.rulesengine.client.RestClient;
import com.rulesengine.client.config.ApiKeyAuthConfig;
import com.rulesengine.client.config.AuthConfig;
import com.rulesengine.client.config.BearerTokenAuthConfig;
import com.rulesengine.fixtures.TestDataFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for external service integrations.
 * Tests the actual HTTP communication with external GraphQL and REST services.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Disabled due to WireMock compatibility issues with Jakarta EE")
class ExternalServiceContractTest {

    private static WireMockServer wireMockServer;
    @Autowired
    private GraphQLClient graphQLClient;
    
    @Autowired
    private RestClient restClient;
    private TestDataFixtures fixtures;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void tearDownWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        fixtures = new TestDataFixtures();
        objectMapper = new ObjectMapper();
        wireMockServer.resetAll();
    }

    // ========== GraphQL Contract Tests ==========

    @Test
    @Order(1)
    @DisplayName("GraphQL Client - Successful Query with API Key Authentication")
    void testGraphQLClientSuccessfulQueryWithApiKey() {
        // Arrange
        String expectedResponse = fixtures.createMockGraphQLResponse();
        
        stubFor(post(urlEqualTo("/graphql"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("X-API-Key", equalTo("test-api-key"))
                .withRequestBody(containing("GetCustomer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(expectedResponse)));

        String endpoint = "http://localhost:8089/graphql";
        String query = "query GetCustomer($id: ID!) { customer(id: $id) { creditScore } }";
        Map<String, Object> variables = Map.of("id", "customer-123");
        
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig();
        authConfig.setApiKey("test-api-key");
        authConfig.setHeaderName("X-API-Key");

        // Act
        Object result = graphQLClient.executeQuery(endpoint, query, variables, authConfig);

        // Assert
        assertNotNull(result);
        verify(postRequestedFor(urlEqualTo("/graphql"))
                .withHeader("X-API-Key", equalTo("test-api-key")));
    }

    @Test
    @Order(2)
    @DisplayName("GraphQL Client - Query with Bearer Token Authentication")
    void testGraphQLClientWithBearerToken() {
        // Arrange
        String expectedResponse = fixtures.createMockGraphQLResponse();
        
        stubFor(post(urlEqualTo("/graphql"))
                .withHeader("Authorization", equalTo("Bearer test-bearer-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(expectedResponse)));

        String endpoint = "http://localhost:8089/graphql";
        String query = "query GetCustomer($id: ID!) { customer(id: $id) { creditScore } }";
        Map<String, Object> variables = Map.of("id", "customer-456");
        
        BearerTokenAuthConfig authConfig = new BearerTokenAuthConfig();
        authConfig.setToken("test-bearer-token");

        // Act
        Object result = graphQLClient.executeQuery(endpoint, query, variables, authConfig);

        // Assert
        assertNotNull(result);
        verify(postRequestedFor(urlEqualTo("/graphql"))
                .withHeader("Authorization", equalTo("Bearer test-bearer-token")));
    }

    @Test
    @Order(3)
    @DisplayName("GraphQL Client - Handle Authentication Error")
    void testGraphQLClientAuthenticationError() {
        // Arrange
        stubFor(post(urlEqualTo("/graphql"))
                .withHeader("X-API-Key", equalTo("invalid-key"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Invalid API key\"}")));

        String endpoint = "http://localhost:8089/graphql";
        String query = "query GetCustomer($id: ID!) { customer(id: $id) { creditScore } }";
        Map<String, Object> variables = Map.of("id", "customer-123");
        
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig();
        authConfig.setApiKey("invalid-key");
        authConfig.setHeaderName("X-API-Key");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            graphQLClient.executeQuery(endpoint, query, variables, authConfig);
        });
    }

    @Test
    @Order(4)
    @DisplayName("GraphQL Client - Handle GraphQL Errors")
    void testGraphQLClientGraphQLErrors() {
        // Arrange
        String errorResponse = """
            {
              "errors": [
                {
                  "message": "Field 'creditScore' not found on type 'Customer'",
                  "locations": [{"line": 1, "column": 45}],
                  "path": ["customer", "creditScore"]
                }
              ]
            }
            """;
        
        stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorResponse)));

        String endpoint = "http://localhost:8089/graphql";
        String query = "query GetCustomer($id: ID!) { customer(id: $id) { creditScore } }";
        Map<String, Object> variables = Map.of("id", "customer-123");
        
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig();
        authConfig.setApiKey("test-api-key");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            graphQLClient.executeQuery(endpoint, query, variables, authConfig);
        });
    }

    // ========== REST Client Contract Tests ==========

    @Test
    @Order(5)
    @DisplayName("REST Client - Successful GET Request with Bearer Token")
    void testRestClientSuccessfulGetRequest() {
        // Arrange
        String expectedResponse = fixtures.createMockRestResponse();
        
        stubFor(get(urlEqualTo("/customers/customer-123"))
                .withHeader("Authorization", equalTo("Bearer test-bearer-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(expectedResponse)));

        String endpoint = "http://localhost:8089/customers/customer-123";
        
        BearerTokenAuthConfig authConfig = new BearerTokenAuthConfig();
        authConfig.setToken("test-bearer-token");

        // Act
        Object result = restClient.executeRequest("GET", endpoint, null, authConfig);

        // Assert
        assertNotNull(result);
        verify(getRequestedFor(urlEqualTo("/customers/customer-123"))
                .withHeader("Authorization", equalTo("Bearer test-bearer-token")));
    }

    @Test
    @Order(6)
    @DisplayName("REST Client - POST Request with JSON Body")
    void testRestClientPostRequest() {
        // Arrange
        String requestBody = """
            {
              "name": "John Doe",
              "age": 30,
              "type": "PREMIUM"
            }
            """;
        
        String responseBody = """
            {
              "id": "customer-789",
              "name": "John Doe",
              "age": 30,
              "type": "PREMIUM",
              "status": "CREATED"
            }
            """;
        
        stubFor(post(urlEqualTo("/customers"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("X-API-Key", equalTo("test-api-key"))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        String endpoint = "http://localhost:8089/customers";
        
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig();
        authConfig.setApiKey("test-api-key");
        authConfig.setHeaderName("X-API-Key");

        // Act
        Object result = restClient.executeRequest("POST", endpoint, requestBody, authConfig);

        // Assert
        assertNotNull(result);
        verify(postRequestedFor(urlEqualTo("/customers"))
                .withHeader("X-API-Key", equalTo("test-api-key"))
                .withRequestBody(equalToJson(requestBody)));
    }

    @Test
    @Order(7)
    @DisplayName("REST Client - Handle 404 Not Found")
    void testRestClientNotFound() {
        // Arrange
        stubFor(get(urlEqualTo("/customers/nonexistent"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Customer not found\"}")));

        String endpoint = "http://localhost:8089/customers/nonexistent";
        
        BearerTokenAuthConfig authConfig = new BearerTokenAuthConfig();
        authConfig.setToken("test-bearer-token");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            restClient.executeRequest("GET", endpoint, null, authConfig);
        });
    }

    @Test
    @Order(8)
    @DisplayName("REST Client - Handle Timeout")
    void testRestClientTimeout() {
        // Arrange
        stubFor(get(urlEqualTo("/customers/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(35000) // 35 seconds delay
                        .withBody("{\"id\":\"customer-slow\"}")));

        String endpoint = "http://localhost:8089/customers/slow";
        
        BearerTokenAuthConfig authConfig = new BearerTokenAuthConfig();
        authConfig.setToken("test-bearer-token");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            restClient.executeRequest("GET", endpoint, null, authConfig);
        });
    }

    // ========== Connection Validation Tests ==========

    @Test
    @Order(9)
    @DisplayName("GraphQL Client - Connection Validation Success")
    void testGraphQLConnectionValidationSuccess() {
        // Arrange
        stubFor(post(urlEqualTo("/graphql"))
                .withRequestBody(containing("__schema"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"__schema\":{\"types\":[]}}}")));

        String endpoint = "http://localhost:8089/graphql";
        
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig();
        authConfig.setApiKey("test-api-key");

        // Act & Assert
        assertDoesNotThrow(() -> {
            graphQLClient.validateConnection(endpoint, authConfig);
        });
    }

    @Test
    @Order(10)
    @DisplayName("REST Client - Connection Validation Success")
    void testRestConnectionValidationSuccess() {
        // Arrange
        stubFor(head(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)));

        String endpoint = "http://localhost:8089/health";
        
        BearerTokenAuthConfig authConfig = new BearerTokenAuthConfig();
        authConfig.setToken("test-bearer-token");

        // Act & Assert
        assertDoesNotThrow(() -> {
            restClient.validateConnection(endpoint, authConfig);
        });
    }

    // ========== Performance Contract Tests ==========

    @Test
    @Order(11)
    @DisplayName("GraphQL Client - Performance Under Load")
    void testGraphQLClientPerformance() {
        // Arrange
        String response = fixtures.createMockGraphQLResponse();
        
        stubFor(post(urlEqualTo("/graphql"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                        .withFixedDelay(100))); // 100ms response time

        String endpoint = "http://localhost:8089/graphql";
        String query = "query GetCustomer($id: ID!) { customer(id: $id) { creditScore } }";
        
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig();
        authConfig.setApiKey("test-api-key");

        // Act - Execute multiple requests
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> variables = Map.of("id", "customer-" + i);
            Object result = graphQLClient.executeQuery(endpoint, query, variables, authConfig);
            assertNotNull(result);
        }
        long endTime = System.currentTimeMillis();

        // Assert - Should complete within reasonable time (allowing for some overhead)
        long totalTime = endTime - startTime;
        assertTrue(totalTime < 5000, "10 requests should complete within 5 seconds, took: " + totalTime + "ms");
        
        // Verify all requests were made
        verify(10, postRequestedFor(urlEqualTo("/graphql")));
    }

    @Test
    @Order(12)
    @DisplayName("REST Client - Concurrent Request Handling")
    void testRestClientConcurrentRequests() {
        // Arrange
        String response = fixtures.createMockRestResponse();
        
        stubFor(get(urlMatching("/customers/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));

        BearerTokenAuthConfig authConfig = new BearerTokenAuthConfig();
        authConfig.setToken("test-bearer-token");

        // Act - Execute concurrent requests
        assertDoesNotThrow(() -> {
            java.util.concurrent.CompletableFuture<?>[] futures = new java.util.concurrent.CompletableFuture[5];
            
            for (int i = 0; i < 5; i++) {
                final int customerId = i;
                futures[i] = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    String endpoint = "http://localhost:8089/customers/customer-" + customerId;
                    Object result = restClient.executeRequest("GET", endpoint, null, authConfig);
                    assertNotNull(result);
                });
            }
            
            // Wait for all requests to complete
            java.util.concurrent.CompletableFuture.allOf(futures).get();
        });

        // Verify all requests were made
        verify(5, getRequestedFor(urlMatching("/customers/.*")));
    }

    // ========== Error Recovery Contract Tests ==========

    @Test
    @Order(13)
    @DisplayName("GraphQL Client - Retry on Temporary Failure")
    void testGraphQLClientRetryOnFailure() {
        // Arrange - First request fails, second succeeds
        stubFor(post(urlEqualTo("/graphql"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"error\":\"Internal Server Error\"}"))
                .willSetStateTo("First Attempt Failed"));
        
        stubFor(post(urlEqualTo("/graphql"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Attempt Failed")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fixtures.createMockGraphQLResponse())));

        String endpoint = "http://localhost:8089/graphql";
        String query = "query GetCustomer($id: ID!) { customer(id: $id) { creditScore } }";
        Map<String, Object> variables = Map.of("id", "customer-retry");
        
        ApiKeyAuthConfig authConfig = new ApiKeyAuthConfig();
        authConfig.setApiKey("test-api-key");

        // Act - Should succeed after retry
        Object result = graphQLClient.executeQueryWithRetry(endpoint, query, variables, authConfig, 2);

        // Assert
        assertNotNull(result);
        verify(2, postRequestedFor(urlEqualTo("/graphql")));
    }
}