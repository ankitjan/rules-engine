package com.rulesengine.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.fixtures.TestDataFixtures;
import com.rulesengine.dto.CreateRuleRequest;
import com.rulesengine.dto.CreateFieldConfigRequest;
import com.rulesengine.dto.ExecutionContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for API endpoints with comprehensive documentation validation.
 * Tests all REST endpoints with various scenarios including success, error, and edge cases.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.yml")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class ApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFixtures fixtures;

    // ========== OpenAPI Documentation Tests ==========

    @Test
    @Order(1)
    @DisplayName("OpenAPI Documentation - API Docs Endpoint Available")
    void testApiDocsEndpointAvailable() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Rules Engine API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andDo(print());
    }

    @Test
    @Order(2)
    @DisplayName("OpenAPI Documentation - Swagger UI Available")
    void testSwaggerUIAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @Order(3)
    @DisplayName("OpenAPI Documentation - Security Schemes Defined")
    void testSecuritySchemesInDocumentation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("bearerAuth"), "Security scheme should be documented");
    }

    // ========== Rule Management API Tests ==========

    @Test
    @Order(4)
    @WithMockUser(roles = "USER")
    @DisplayName("Rule API - Create Rule Success")
    void testCreateRuleSuccess() throws Exception {
        CreateRuleRequest request = fixtures.createSimpleRuleRequest();
        
        mockMvc.perform(post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andDo(print());
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "USER")
    @DisplayName("Rule API - Create Rule Validation Error")
    void testCreateRuleValidationError() throws Exception {
        CreateRuleRequest request = new CreateRuleRequest();
        // Missing required fields to trigger validation error
        
        mockMvc.perform(post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "USER")
    @DisplayName("Rule API - Get Rules with Pagination")
    void testGetRulesWithPagination() throws Exception {
        mockMvc.perform(get("/api/rules")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "name")
                .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0))
                .andDo(print());
    }

    @Test
    @Order(7)
    @WithMockUser(roles = "USER")
    @DisplayName("Rule API - Get Rule by ID Not Found")
    void testGetRuleByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/rules/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andDo(print());
    }

    @Test
    @Order(8)
    @WithMockUser(roles = "USER")
    @DisplayName("Rule API - Execute Rule Success")
    void testExecuteRuleSuccess() throws Exception {
        // First create a rule
        CreateRuleRequest createRequest = fixtures.createSimpleRuleRequest();
        MvcResult createResult = mockMvc.perform(post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        String createResponse = createResult.getResponse().getContentAsString();
        Long ruleId = objectMapper.readTree(createResponse).get("id").asLong();
        
        // Execute the rule
        ExecutionContext context = fixtures.createSimpleExecutionContext();
        
        mockMvc.perform(post("/api/rules/" + ruleId + "/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ruleId").value(ruleId))
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.executionTimeMs").exists())
                .andDo(print());
    }

    @Test
    @Order(9)
    @DisplayName("Rule API - Unauthorized Access")
    void testRuleApiUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/rules"))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }

    // ========== Field Configuration API Tests ==========

    @Test
    @Order(10)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Field Config API - Create Field Configuration Success")
    void testCreateFieldConfigSuccess() throws Exception {
        CreateFieldConfigRequest request = fixtures.createStringFieldConfigRequest();
        
        mockMvc.perform(post("/api/field-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fieldName").value(request.getFieldName()))
                .andExpect(jsonPath("$.fieldType").value(request.getFieldType()))
                .andDo(print());
    }

    @Test
    @Order(11)
    @WithMockUser(roles = "USER")
    @DisplayName("Field Config API - Get All Field Configurations")
    void testGetAllFieldConfigurations() throws Exception {
        mockMvc.perform(get("/api/field-configs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andDo(print());
    }

    @Test
    @Order(12)
    @WithMockUser(roles = "USER")
    @DisplayName("Field Config API - Insufficient Permissions")
    void testFieldConfigInsufficientPermissions() throws Exception {
        CreateFieldConfigRequest request = fixtures.createStringFieldConfigRequest();
        
        mockMvc.perform(post("/api/field-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    // ========== Health Check API Tests ==========

    @Test
    @Order(13)
    @DisplayName("Health API - Health Check Endpoint")
    void testHealthCheckEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andDo(print());
    }

    @Test
    @Order(14)
    @DisplayName("Health API - Application Info Endpoint")
    void testApplicationInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    // ========== Error Handling Tests ==========

    @Test
    @Order(15)
    @WithMockUser(roles = "USER")
    @DisplayName("Error Handling - Invalid JSON Request")
    void testInvalidJsonRequest() throws Exception {
        String invalidJson = "{ invalid json }";
        
        mockMvc.perform(post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    @Order(16)
    @WithMockUser(roles = "USER")
    @DisplayName("Error Handling - Unsupported Media Type")
    void testUnsupportedMediaType() throws Exception {
        CreateRuleRequest request = fixtures.createSimpleRuleRequest();
        
        mockMvc.perform(post("/api/rules")
                .contentType(MediaType.TEXT_PLAIN)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType())
                .andDo(print());
    }

    @Test
    @Order(17)
    @WithMockUser(roles = "USER")
    @DisplayName("Error Handling - Method Not Allowed")
    void testMethodNotAllowed() throws Exception {
        mockMvc.perform(patch("/api/rules"))
                .andExpect(status().isMethodNotAllowed())
                .andDo(print());
    }

    // ========== CORS Tests ==========

    @Test
    @Order(18)
    @DisplayName("CORS - Preflight Request")
    void testCorsPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/rules")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andDo(print());
    }

    // ========== Content Negotiation Tests ==========

    @Test
    @Order(19)
    @WithMockUser(roles = "USER")
    @DisplayName("Content Negotiation - Accept JSON")
    void testContentNegotiationJson() throws Exception {
        mockMvc.perform(get("/api/rules")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    @Test
    @Order(20)
    @WithMockUser(roles = "USER")
    @DisplayName("Content Negotiation - Unsupported Accept Header")
    void testContentNegotiationUnsupported() throws Exception {
        mockMvc.perform(get("/api/rules")
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andDo(print());
    }

    // ========== Rate Limiting Tests ==========

    @Test
    @Order(21)
    @WithMockUser(roles = "USER")
    @DisplayName("Rate Limiting - Multiple Requests Within Limit")
    void testRateLimitingWithinLimit() throws Exception {
        // Make multiple requests quickly
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/rules"))
                    .andExpect(status().isOk());
        }
    }

    // ========== API Versioning Tests ==========

    @Test
    @Order(22)
    @WithMockUser(roles = "USER")
    @DisplayName("API Versioning - Version Header Support")
    void testApiVersioningSupport() throws Exception {
        mockMvc.perform(get("/api/rules")
                .header("API-Version", "1.0"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    // ========== Performance Tests ==========

    @Test
    @Order(23)
    @WithMockUser(roles = "USER")
    @DisplayName("Performance - Response Time Validation")
    void testResponseTimeValidation() throws Exception {
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/rules"))
                .andExpect(status().isOk());
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        assertTrue(responseTime < 5000, "Response time should be under 5 seconds, was: " + responseTime + "ms");
    }

    // ========== Documentation Completeness Tests ==========

    @Test
    @Order(24)
    @DisplayName("Documentation - All Endpoints Documented")
    void testAllEndpointsDocumented() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        
        String apiDocs = result.getResponse().getContentAsString();
        
        // Verify key endpoints are documented
        assertTrue(apiDocs.contains("/api/rules"), "Rules endpoints should be documented");
        assertTrue(apiDocs.contains("/api/field-configs"), "Field config endpoints should be documented");
        assertTrue(apiDocs.contains("POST"), "POST operations should be documented");
        assertTrue(apiDocs.contains("GET"), "GET operations should be documented");
        assertTrue(apiDocs.contains("PUT"), "PUT operations should be documented");
        assertTrue(apiDocs.contains("DELETE"), "DELETE operations should be documented");
    }

    @Test
    @Order(25)
    @DisplayName("Documentation - Response Schemas Defined")
    void testResponseSchemasDefined() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        
        String apiDocs = result.getResponse().getContentAsString();
        
        // Verify response schemas are defined
        assertTrue(apiDocs.contains("RuleResponse"), "RuleResponse schema should be defined");
        assertTrue(apiDocs.contains("FieldConfigResponse"), "FieldConfigResponse schema should be defined");
        assertTrue(apiDocs.contains("RuleExecutionResult"), "RuleExecutionResult schema should be defined");
    }

    @Test
    @Order(26)
    @DisplayName("Documentation - Error Response Examples")
    void testErrorResponseExamples() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        
        String apiDocs = result.getResponse().getContentAsString();
        
        // Verify error responses are documented
        assertTrue(apiDocs.contains("400"), "Bad Request responses should be documented");
        assertTrue(apiDocs.contains("401"), "Unauthorized responses should be documented");
        assertTrue(apiDocs.contains("404"), "Not Found responses should be documented");
        assertTrue(apiDocs.contains("500"), "Internal Server Error responses should be documented");
    }
}