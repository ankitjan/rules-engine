package com.rulesengine.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.fixtures.TestDataFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Generates comprehensive API documentation with examples.
 * Creates markdown files with request/response examples for all endpoints.
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
class ApiDocumentationGenerator {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFixtures fixtures;

    private StringBuilder documentation;
    private Path outputDir;

    @BeforeEach
    void setUp() throws IOException {
        documentation = new StringBuilder();
        outputDir = Paths.get("target/generated-docs");
        Files.createDirectories(outputDir);
        
        documentation.append("# Rules Engine API Documentation\n\n");
        documentation.append("This document provides comprehensive examples for all API endpoints.\n\n");
        documentation.append("## Table of Contents\n\n");
        documentation.append("- [Authentication](#authentication)\n");
        documentation.append("- [Rule Management](#rule-management)\n");
        documentation.append("- [Field Configuration](#field-configuration)\n");
        documentation.append("- [Rule Execution](#rule-execution)\n");
        documentation.append("- [Error Handling](#error-handling)\n\n");
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateCompleteApiDocumentation() throws Exception {
        generateAuthenticationSection();
        generateRuleManagementSection();
        generateFieldConfigurationSection();
        generateRuleExecutionSection();
        generateErrorHandlingSection();
        
        writeDocumentationToFile();
    }

    private void generateAuthenticationSection() {
        documentation.append("## Authentication\n\n");
        documentation.append("The Rules Engine API uses JWT (JSON Web Token) for authentication.\n\n");
        documentation.append("### Request Headers\n\n");
        documentation.append("```http\n");
        documentation.append("Authorization: Bearer <your-jwt-token>\n");
        documentation.append("Content-Type: application/json\n");
        documentation.append("```\n\n");
        documentation.append("### Example JWT Token\n\n");
        documentation.append("```\n");
        documentation.append("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\n");
        documentation.append("```\n\n");
    }

    private void generateRuleManagementSection() throws Exception {
        documentation.append("## Rule Management\n\n");
        documentation.append("APIs for creating, updating, and managing business rules.\n\n");
        
        // Create Rule
        documentation.append("### Create Rule\n\n");
        documentation.append("Creates a new business rule.\n\n");
        documentation.append("**Endpoint:** `POST /api/rules`\n\n");
        documentation.append("**Request Body:**\n\n");
        documentation.append("```json\n");
        documentation.append(objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(fixtures.createSimpleRuleRequest()));
        documentation.append("\n```\n\n");
        
        // Execute create rule request to get response
        MvcResult createResult = mockMvc.perform(post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fixtures.createSimpleRuleRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        
        documentation.append("**Response (201 Created):**\n\n");
        documentation.append("```json\n");
        documentation.append(formatJson(createResult.getResponse().getContentAsString()));
        documentation.append("\n```\n\n");
        
        // Get Rules
        documentation.append("### Get Rules\n\n");
        documentation.append("Retrieves all rules with pagination support.\n\n");
        documentation.append("**Endpoint:** `GET /api/rules?page=0&size=10&sortBy=name&sortDir=asc`\n\n");
        
        MvcResult getRulesResult = mockMvc.perform(get("/api/rules")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        
        documentation.append("**Response (200 OK):**\n\n");
        documentation.append("```json\n");
        documentation.append(formatJson(getRulesResult.getResponse().getContentAsString()));
        documentation.append("\n```\n\n");
        
        // Get Rule by ID
        Long ruleId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();
        
        documentation.append("### Get Rule by ID\n\n");
        documentation.append("Retrieves a specific rule by its ID.\n\n");
        documentation.append("**Endpoint:** `GET /api/rules/{id}`\n\n");
        
        MvcResult getRuleResult = mockMvc.perform(get("/api/rules/" + ruleId))
                .andExpect(status().isOk())
                .andReturn();
        
        documentation.append("**Response (200 OK):**\n\n");
        documentation.append("```json\n");
        documentation.append(formatJson(getRuleResult.getResponse().getContentAsString()));
        documentation.append("\n```\n\n");
        
        // Update Rule
        documentation.append("### Update Rule\n\n");
        documentation.append("Updates an existing rule.\n\n");
        documentation.append("**Endpoint:** `PUT /api/rules/{id}`\n\n");
        documentation.append("**Request Body:**\n\n");
        documentation.append("```json\n");
        documentation.append("{\n");
        documentation.append("  \"name\": \"Updated Customer Age Validation\",\n");
        documentation.append("  \"description\": \"Updated validation rule for customer age\",\n");
        documentation.append("  \"ruleDefinitionJson\": \"{...}\"\n");
        documentation.append("}\n");
        documentation.append("```\n\n");
        
        // Delete Rule
        documentation.append("### Delete Rule\n\n");
        documentation.append("Deletes a rule (soft delete).\n\n");
        documentation.append("**Endpoint:** `DELETE /api/rules/{id}`\n\n");
        documentation.append("**Response:** `204 No Content`\n\n");
    }

    private void generateFieldConfigurationSection() throws Exception {
        documentation.append("## Field Configuration\n\n");
        documentation.append("APIs for managing field configurations and data service integrations.\n\n");
        
        // Create Field Configuration
        documentation.append("### Create Field Configuration\n\n");
        documentation.append("Creates a new field configuration.\n\n");
        documentation.append("**Endpoint:** `POST /api/field-configs`\n\n");
        
        // Simple field configuration
        documentation.append("#### Simple String Field\n\n");
        documentation.append("```json\n");
        documentation.append(objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(fixtures.createStringFieldConfigRequest()));
        documentation.append("\n```\n\n");
        
        // GraphQL field configuration
        documentation.append("#### GraphQL Field Configuration\n\n");
        documentation.append("```json\n");
        documentation.append(objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(fixtures.createGraphQLFieldConfigRequest()));
        documentation.append("\n```\n\n");
        
        // Calculated field configuration
        documentation.append("#### Calculated Field Configuration\n\n");
        documentation.append("```json\n");
        documentation.append(objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(fixtures.createCalculatedFieldConfigRequest()));
        documentation.append("\n```\n\n");
        
        // Get Field Configurations
        documentation.append("### Get Field Configurations\n\n");
        documentation.append("Retrieves all field configurations.\n\n");
        documentation.append("**Endpoint:** `GET /api/field-configs`\n\n");
        
        MvcResult getFieldConfigsResult = mockMvc.perform(get("/api/field-configs"))
                .andExpect(status().isOk())
                .andReturn();
        
        documentation.append("**Response (200 OK):**\n\n");
        documentation.append("```json\n");
        documentation.append(formatJson(getFieldConfigsResult.getResponse().getContentAsString()));
        documentation.append("\n```\n\n");
    }

    private void generateRuleExecutionSection() throws Exception {
        documentation.append("## Rule Execution\n\n");
        documentation.append("APIs for executing rules against data contexts.\n\n");
        
        // Execute Rule
        documentation.append("### Execute Rule\n\n");
        documentation.append("Executes a specific rule against provided data context.\n\n");
        documentation.append("**Endpoint:** `POST /api/rules/{id}/execute`\n\n");
        documentation.append("**Request Body:**\n\n");
        documentation.append("```json\n");
        documentation.append(objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(fixtures.createSimpleExecutionContext()));
        documentation.append("\n```\n\n");
        
        // Batch Execution
        documentation.append("### Batch Rule Execution\n\n");
        documentation.append("Executes multiple rules against the same data context.\n\n");
        documentation.append("**Endpoint:** `POST /api/rules/execute-batch`\n\n");
        documentation.append("**Request Body:**\n\n");
        documentation.append("```json\n");
        documentation.append(objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(fixtures.createBatchExecutionRequest()));
        documentation.append("\n```\n\n");
        
        // Rule Validation
        documentation.append("### Rule Validation\n\n");
        documentation.append("Validates a rule definition without persisting it.\n\n");
        documentation.append("**Endpoint:** `POST /api/rules/validate`\n\n");
        documentation.append("**Request Body:**\n\n");
        documentation.append("```json\n");
        documentation.append(fixtures.createSimpleRuleDefinitionJson());
        documentation.append("\n```\n\n");
    }

    private void generateErrorHandlingSection() throws Exception {
        documentation.append("## Error Handling\n\n");
        documentation.append("The API returns standard HTTP status codes and structured error responses.\n\n");
        
        // Validation Error
        documentation.append("### Validation Error (400 Bad Request)\n\n");
        documentation.append("```json\n");
        documentation.append("{\n");
        documentation.append("  \"error\": \"Validation failed\",\n");
        documentation.append("  \"message\": \"Field name must be unique\",\n");
        documentation.append("  \"timestamp\": \"2023-12-01T10:00:00Z\",\n");
        documentation.append("  \"path\": \"/api/field-configs\",\n");
        documentation.append("  \"details\": [\n");
        documentation.append("    {\n");
        documentation.append("      \"field\": \"fieldName\",\n");
        documentation.append("      \"rejectedValue\": \"duplicateName\",\n");
        documentation.append("      \"message\": \"Field name must be unique\"\n");
        documentation.append("    }\n");
        documentation.append("  ]\n");
        documentation.append("}\n");
        documentation.append("```\n\n");
        
        // Not Found Error
        documentation.append("### Not Found Error (404 Not Found)\n\n");
        documentation.append("```json\n");
        documentation.append("{\n");
        documentation.append("  \"error\": \"Not Found\",\n");
        documentation.append("  \"message\": \"Rule with ID 999 not found\",\n");
        documentation.append("  \"timestamp\": \"2023-12-01T10:00:00Z\",\n");
        documentation.append("  \"path\": \"/api/rules/999\"\n");
        documentation.append("}\n");
        documentation.append("```\n\n");
        
        // Unauthorized Error
        documentation.append("### Unauthorized Error (401 Unauthorized)\n\n");
        documentation.append("```json\n");
        documentation.append("{\n");
        documentation.append("  \"error\": \"Unauthorized\",\n");
        documentation.append("  \"message\": \"JWT token is missing or invalid\",\n");
        documentation.append("  \"timestamp\": \"2023-12-01T10:00:00Z\",\n");
        documentation.append("  \"path\": \"/api/rules\"\n");
        documentation.append("}\n");
        documentation.append("```\n\n");
        
        // Internal Server Error
        documentation.append("### Internal Server Error (500 Internal Server Error)\n\n");
        documentation.append("```json\n");
        documentation.append("{\n");
        documentation.append("  \"error\": \"Internal Server Error\",\n");
        documentation.append("  \"message\": \"An unexpected error occurred\",\n");
        documentation.append("  \"timestamp\": \"2023-12-01T10:00:00Z\",\n");
        documentation.append("  \"path\": \"/api/rules\",\n");
        documentation.append("  \"traceId\": \"abc123def456\"\n");
        documentation.append("}\n");
        documentation.append("```\n\n");
    }

    private String formatJson(String json) throws Exception {
        Object jsonObject = objectMapper.readValue(json, Object.class);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    }

    private void writeDocumentationToFile() throws IOException {
        Path docFile = outputDir.resolve("api-documentation.md");
        try (FileWriter writer = new FileWriter(docFile.toFile())) {
            writer.write(documentation.toString());
        }
        
        System.out.println("API documentation generated: " + docFile.toAbsolutePath());
    }

    @Test
    void generateOpenApiSpecFile() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        
        String openApiSpec = formatJson(result.getResponse().getContentAsString());
        
        Path specFile = outputDir.resolve("openapi-spec.json");
        try (FileWriter writer = new FileWriter(specFile.toFile())) {
            writer.write(openApiSpec);
        }
        
        System.out.println("OpenAPI specification generated: " + specFile.toAbsolutePath());
    }

    @Test
    void generatePostmanCollection() throws Exception {
        StringBuilder postmanCollection = new StringBuilder();
        
        postmanCollection.append("{\n");
        postmanCollection.append("  \"info\": {\n");
        postmanCollection.append("    \"name\": \"Rules Engine API\",\n");
        postmanCollection.append("    \"description\": \"Comprehensive API collection for Rules Engine\",\n");
        postmanCollection.append("    \"version\": \"1.0.0\"\n");
        postmanCollection.append("  },\n");
        postmanCollection.append("  \"auth\": {\n");
        postmanCollection.append("    \"type\": \"bearer\",\n");
        postmanCollection.append("    \"bearer\": [\n");
        postmanCollection.append("      {\n");
        postmanCollection.append("        \"key\": \"token\",\n");
        postmanCollection.append("        \"value\": \"{{jwt_token}}\",\n");
        postmanCollection.append("        \"type\": \"string\"\n");
        postmanCollection.append("      }\n");
        postmanCollection.append("    ]\n");
        postmanCollection.append("  },\n");
        postmanCollection.append("  \"variable\": [\n");
        postmanCollection.append("    {\n");
        postmanCollection.append("      \"key\": \"base_url\",\n");
        postmanCollection.append("      \"value\": \"http://localhost:8080/api\"\n");
        postmanCollection.append("    },\n");
        postmanCollection.append("    {\n");
        postmanCollection.append("      \"key\": \"jwt_token\",\n");
        postmanCollection.append("      \"value\": \"your-jwt-token-here\"\n");
        postmanCollection.append("    }\n");
        postmanCollection.append("  ],\n");
        postmanCollection.append("  \"item\": [\n");
        
        // Add rule management endpoints
        addPostmanRuleEndpoints(postmanCollection);
        
        // Add field configuration endpoints
        addPostmanFieldConfigEndpoints(postmanCollection);
        
        postmanCollection.append("  ]\n");
        postmanCollection.append("}\n");
        
        Path collectionFile = outputDir.resolve("postman-collection.json");
        try (FileWriter writer = new FileWriter(collectionFile.toFile())) {
            writer.write(postmanCollection.toString());
        }
        
        System.out.println("Postman collection generated: " + collectionFile.toAbsolutePath());
    }

    private void addPostmanRuleEndpoints(StringBuilder collection) throws Exception {
        collection.append("    {\n");
        collection.append("      \"name\": \"Rule Management\",\n");
        collection.append("      \"item\": [\n");
        collection.append("        {\n");
        collection.append("          \"name\": \"Create Rule\",\n");
        collection.append("          \"request\": {\n");
        collection.append("            \"method\": \"POST\",\n");
        collection.append("            \"header\": [],\n");
        collection.append("            \"body\": {\n");
        collection.append("              \"mode\": \"raw\",\n");
        collection.append("              \"raw\": ");
        collection.append(objectMapper.writeValueAsString(
                objectMapper.writeValueAsString(fixtures.createSimpleRuleRequest())));
        collection.append(",\n");
        collection.append("              \"options\": {\n");
        collection.append("                \"raw\": {\n");
        collection.append("                  \"language\": \"json\"\n");
        collection.append("                }\n");
        collection.append("              }\n");
        collection.append("            },\n");
        collection.append("            \"url\": {\n");
        collection.append("              \"raw\": \"{{base_url}}/rules\",\n");
        collection.append("              \"host\": [\"{{base_url}}\"],\n");
        collection.append("              \"path\": [\"rules\"]\n");
        collection.append("            }\n");
        collection.append("          }\n");
        collection.append("        }\n");
        collection.append("      ]\n");
        collection.append("    }");
    }

    private void addPostmanFieldConfigEndpoints(StringBuilder collection) throws Exception {
        collection.append(",\n");
        collection.append("    {\n");
        collection.append("      \"name\": \"Field Configuration\",\n");
        collection.append("      \"item\": [\n");
        collection.append("        {\n");
        collection.append("          \"name\": \"Create Field Config\",\n");
        collection.append("          \"request\": {\n");
        collection.append("            \"method\": \"POST\",\n");
        collection.append("            \"header\": [],\n");
        collection.append("            \"body\": {\n");
        collection.append("              \"mode\": \"raw\",\n");
        collection.append("              \"raw\": ");
        collection.append(objectMapper.writeValueAsString(
                objectMapper.writeValueAsString(fixtures.createStringFieldConfigRequest())));
        collection.append(",\n");
        collection.append("              \"options\": {\n");
        collection.append("                \"raw\": {\n");
        collection.append("                  \"language\": \"json\"\n");
        collection.append("                }\n");
        collection.append("              }\n");
        collection.append("            },\n");
        collection.append("            \"url\": {\n");
        collection.append("              \"raw\": \"{{base_url}}/field-configs\",\n");
        collection.append("              \"host\": [\"{{base_url}}\"],\n");
        collection.append("              \"path\": [\"field-configs\"]\n");
        collection.append("            }\n");
        collection.append("          }\n");
        collection.append("        }\n");
        collection.append("      ]\n");
        collection.append("    }\n");
    }
}