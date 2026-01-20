package com.rulesengine.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.BatchExecutionRequest;
import com.rulesengine.dto.CreateRuleRequest;
import com.rulesengine.dto.ExecutionContext;
import com.rulesengine.dto.RuleExecutionResult;
import com.rulesengine.dto.RuleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class RuleExecutionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testRuleId;

    @BeforeEach
    void setUp() throws Exception {
        // Create a test rule
        CreateRuleRequest createRequest = new CreateRuleRequest();
        createRequest.setName("Test Age Rule");
        createRequest.setDescription("Test rule for age validation");
        createRequest.setRuleDefinitionJson("""
            {
                "combinator": "and",
                "rules": [
                    {
                        "field": "age",
                        "operator": ">=",
                        "value": 18
                    }
                ]
            }
            """);

        MvcResult createResult = mockMvc.perform(post("/api/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        RuleResponse ruleResponse = objectMapper.readValue(responseJson, RuleResponse.class);
        testRuleId = ruleResponse.getId();
    }

    @Test
    @WithMockUser(roles = "USER")
    void testExecuteRule_Success() throws Exception {
        // Given
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 25);
        fieldValues.put("status", "active");
        
        ExecutionContext context = new ExecutionContext("user123", "User", fieldValues);

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/rules/{id}/execute", testRuleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value(testRuleId))
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.hasError").value(false))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        RuleExecutionResult executionResult = objectMapper.readValue(responseJson, RuleExecutionResult.class);
        
        assertNotNull(executionResult);
        assertEquals(testRuleId, executionResult.getRuleId());
        assertTrue(executionResult.isResult());
        assertFalse(executionResult.isHasError());
        assertNotNull(executionResult.getTraces());
        assertTrue(executionResult.getExecutionDurationMs() >= 0);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testExecuteRule_Failure() throws Exception {
        // Given - age below 18 should fail the rule
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 16);
        fieldValues.put("status", "active");
        
        ExecutionContext context = new ExecutionContext("user456", "User", fieldValues);

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/rules/{id}/execute", testRuleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value(testRuleId))
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.hasError").value(false))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        RuleExecutionResult executionResult = objectMapper.readValue(responseJson, RuleExecutionResult.class);
        
        assertNotNull(executionResult);
        assertEquals(testRuleId, executionResult.getRuleId());
        assertFalse(executionResult.isResult());
        assertFalse(executionResult.isHasError());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testExecuteBatch_Success() throws Exception {
        // Given
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 25);
        fieldValues.put("status", "active");
        
        ExecutionContext context = new ExecutionContext("user789", "User", fieldValues);
        BatchExecutionRequest batchRequest = new BatchExecutionRequest(List.of(testRuleId), context);

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/rules/execute-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ruleId").value(testRuleId))
                .andExpect(jsonPath("$[0].result").value(true))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        List<RuleExecutionResult> results = objectMapper.readValue(responseJson, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, RuleExecutionResult.class));
        
        assertNotNull(results);
        assertEquals(1, results.size());
        
        RuleExecutionResult executionResult = results.get(0);
        assertEquals(testRuleId, executionResult.getRuleId());
        assertTrue(executionResult.isResult());
        assertFalse(executionResult.isHasError());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testValidateRuleExecution_Success() throws Exception {
        // Given
        String ruleDefinitionJson = """
            {
                "combinator": "and",
                "rules": [
                    {
                        "field": "score",
                        "operator": ">",
                        "value": 80
                    }
                ]
            }
            """;
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("score", 85);
        
        ExecutionContext context = new ExecutionContext("test123", "Test", fieldValues);

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/rules/validate-execution")
                .param("ruleDefinitionJson", ruleDefinitionJson)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value(-1))
                .andExpect(jsonPath("$.ruleName").value("Test Rule"))
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.hasError").value(false))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        RuleExecutionResult executionResult = objectMapper.readValue(responseJson, RuleExecutionResult.class);
        
        assertNotNull(executionResult);
        assertEquals(-1L, executionResult.getRuleId());
        assertEquals("Test Rule", executionResult.getRuleName());
        assertTrue(executionResult.isResult());
        assertFalse(executionResult.isHasError());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testExecuteRule_RuleNotFound() throws Exception {
        // Given
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 25);
        
        ExecutionContext context = new ExecutionContext("user999", "User", fieldValues);

        // When & Then
        mockMvc.perform(post("/api/rules/{id}/execute", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value(999999L))
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.hasError").value(true))
                .andExpect(jsonPath("$.errorMessage").exists());
    }
}