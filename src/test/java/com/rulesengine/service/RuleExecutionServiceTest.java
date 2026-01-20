package com.rulesengine.service;

import com.rulesengine.dto.BatchExecutionRequest;
import com.rulesengine.dto.ExecutionContext;
import com.rulesengine.dto.RuleExecutionResult;
import com.rulesengine.engine.RuleEvaluator;
import com.rulesengine.entity.RuleEntity;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.model.RuleItem;
import com.rulesengine.repository.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleExecutionServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private RuleEvaluator ruleEvaluator;

    @InjectMocks
    private RuleExecutionService ruleExecutionService;

    private RuleEntity testRule;
    private ExecutionContext testContext;
    private RuleDefinition testRuleDefinition;

    @BeforeEach
    void setUp() {
        testRule = new RuleEntity();
        testRule.setId(1L);
        testRule.setName("Test Rule");
        testRule.setRuleDefinitionJson("{\"combinator\":\"and\",\"rules\":[{\"field\":\"age\",\"operator\":\">=\",\"value\":18}]}");

        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 25);
        fieldValues.put("status", "active");
        
        testContext = new ExecutionContext("user123", "User", fieldValues);

        RuleItem condition = new RuleItem("age", ">=", 18);
        testRuleDefinition = new RuleDefinition("and", List.of(condition));
    }

    @Test
    void testExecuteRule_Success() {
        // Given
        when(ruleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRule));
        when(ruleEvaluator.parseRuleDefinition(anyString())).thenReturn(testRuleDefinition);
        
        RuleExecutionResult mockResult = new RuleExecutionResult(1L, "Test Rule", true);
        mockResult.setExecutionDurationMs(50L);
        when(ruleEvaluator.evaluateWithTrace(any(RuleDefinition.class), anyMap())).thenReturn(mockResult);

        // When
        RuleExecutionResult result = ruleExecutionService.executeRule(1L, testContext);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getRuleId());
        assertEquals("Test Rule", result.getRuleName());
        assertTrue(result.isResult());
        assertFalse(result.isHasError());
        assertTrue(result.getExecutionDurationMs() >= 0);

        verify(ruleRepository).findByIdActive(1L);
        verify(ruleEvaluator).parseRuleDefinition(testRule.getRuleDefinitionJson());
        verify(ruleEvaluator).evaluateWithTrace(eq(testRuleDefinition), anyMap());
    }

    @Test
    void testExecuteRule_RuleNotFound() {
        // Given
        when(ruleRepository.findByIdActive(999L)).thenReturn(Optional.empty());

        // When
        RuleExecutionResult result = ruleExecutionService.executeRule(999L, testContext);

        // Then
        assertNotNull(result);
        assertEquals(999L, result.getRuleId());
        assertFalse(result.isResult());
        assertTrue(result.isHasError());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Execution error"));

        verify(ruleRepository).findByIdActive(999L);
        verifyNoInteractions(ruleEvaluator);
    }

    @Test
    void testExecuteRule_EvaluationError() {
        // Given
        when(ruleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRule));
        when(ruleEvaluator.parseRuleDefinition(anyString())).thenThrow(new RuntimeException("Parse error"));

        // When
        RuleExecutionResult result = ruleExecutionService.executeRule(1L, testContext);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getRuleId());
        assertFalse(result.isResult());
        assertTrue(result.isHasError());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Execution error"));

        verify(ruleRepository).findByIdActive(1L);
        verify(ruleEvaluator).parseRuleDefinition(testRule.getRuleDefinitionJson());
    }

    @Test
    void testExecuteBatch_Success() {
        // Given
        RuleEntity rule2 = new RuleEntity();
        rule2.setId(2L);
        rule2.setName("Test Rule 2");
        rule2.setRuleDefinitionJson("{\"combinator\":\"and\",\"rules\":[{\"field\":\"status\",\"operator\":\"=\",\"value\":\"active\"}]}");

        when(ruleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRule));
        when(ruleRepository.findByIdActive(2L)).thenReturn(Optional.of(rule2));
        when(ruleEvaluator.parseRuleDefinition(anyString())).thenReturn(testRuleDefinition);
        
        RuleExecutionResult mockResult1 = new RuleExecutionResult(1L, "Test Rule", true);
        RuleExecutionResult mockResult2 = new RuleExecutionResult(2L, "Test Rule 2", false);
        when(ruleEvaluator.evaluateWithTrace(any(RuleDefinition.class), anyMap()))
            .thenReturn(mockResult1)
            .thenReturn(mockResult2);

        BatchExecutionRequest request = new BatchExecutionRequest(Arrays.asList(1L, 2L), testContext);

        // When
        List<RuleExecutionResult> results = ruleExecutionService.executeBatch(request);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        
        assertEquals(1L, results.get(0).getRuleId());
        assertTrue(results.get(0).isResult());
        
        assertEquals(2L, results.get(1).getRuleId());
        assertFalse(results.get(1).isResult());

        verify(ruleRepository).findByIdActive(1L);
        verify(ruleRepository).findByIdActive(2L);
        verify(ruleEvaluator, times(2)).parseRuleDefinition(anyString());
        verify(ruleEvaluator, times(2)).evaluateWithTrace(any(RuleDefinition.class), anyMap());
    }

    @Test
    void testExecuteBatch_StopOnFirstFailure() {
        // Given
        RuleEntity rule2 = new RuleEntity();
        rule2.setId(2L);
        rule2.setName("Test Rule 2");
        rule2.setRuleDefinitionJson("{\"combinator\":\"and\",\"rules\":[{\"field\":\"status\",\"operator\":\"=\",\"value\":\"active\"}]}");

        when(ruleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRule));
        when(ruleEvaluator.parseRuleDefinition(anyString())).thenReturn(testRuleDefinition);
        
        RuleExecutionResult mockResult1 = new RuleExecutionResult(1L, "Test Rule", false); // First rule fails
        when(ruleEvaluator.evaluateWithTrace(any(RuleDefinition.class), anyMap())).thenReturn(mockResult1);

        BatchExecutionRequest request = new BatchExecutionRequest(Arrays.asList(1L, 2L), testContext);
        request.setStopOnFirstFailure(true);

        // When
        List<RuleExecutionResult> results = ruleExecutionService.executeBatch(request);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size()); // Should stop after first failure
        
        assertEquals(1L, results.get(0).getRuleId());
        assertFalse(results.get(0).isResult());

        verify(ruleRepository).findByIdActive(1L);
        verify(ruleRepository, never()).findByIdActive(2L); // Second rule should not be executed
    }

    @Test
    void testExecuteBatch_WithoutTraces() {
        // Given
        when(ruleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRule));
        when(ruleEvaluator.parseRuleDefinition(anyString())).thenReturn(testRuleDefinition);
        
        RuleExecutionResult mockResult = new RuleExecutionResult(1L, "Test Rule", true);
        mockResult.addTrace("test", "Test trace", true);
        when(ruleEvaluator.evaluateWithTrace(any(RuleDefinition.class), anyMap())).thenReturn(mockResult);

        BatchExecutionRequest request = new BatchExecutionRequest(List.of(1L), testContext);
        request.setIncludeTraces(false);

        // When
        List<RuleExecutionResult> results = ruleExecutionService.executeBatch(request);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getTraces().isEmpty()); // Traces should be removed
    }

    @Test
    void testValidateRuleExecution_Success() {
        // Given
        String ruleJson = "{\"combinator\":\"and\",\"rules\":[{\"field\":\"age\",\"operator\":\">=\",\"value\":18}]}";
        when(ruleEvaluator.parseRuleDefinition(ruleJson)).thenReturn(testRuleDefinition);
        
        RuleExecutionResult mockResult = new RuleExecutionResult(-1L, "Test Rule", true);
        when(ruleEvaluator.evaluateWithTrace(any(RuleDefinition.class), anyMap())).thenReturn(mockResult);

        // When
        RuleExecutionResult result = ruleExecutionService.validateRuleExecution(ruleJson, testContext);

        // Then
        assertNotNull(result);
        assertEquals(-1L, result.getRuleId()); // Test rule indicator
        assertEquals("Test Rule", result.getRuleName());
        assertTrue(result.isResult());
        assertFalse(result.isHasError());

        verify(ruleEvaluator).parseRuleDefinition(ruleJson);
        verify(ruleEvaluator).evaluateWithTrace(eq(testRuleDefinition), anyMap());
    }

    @Test
    void testValidateRuleExecution_ParseError() {
        // Given
        String invalidRuleJson = "{ invalid json }";
        when(ruleEvaluator.parseRuleDefinition(invalidRuleJson)).thenThrow(new RuntimeException("Parse error"));

        // When
        RuleExecutionResult result = ruleExecutionService.validateRuleExecution(invalidRuleJson, testContext);

        // Then
        assertNotNull(result);
        assertEquals(-1L, result.getRuleId());
        assertEquals("Test Rule", result.getRuleName());
        assertFalse(result.isResult());
        assertTrue(result.isHasError());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Validation error"));

        verify(ruleEvaluator).parseRuleDefinition(invalidRuleJson);
    }
}