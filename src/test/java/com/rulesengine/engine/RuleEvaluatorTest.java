package com.rulesengine.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.RuleExecutionResult;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.model.RuleItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RuleEvaluatorTest {

    private RuleEvaluator ruleEvaluator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ruleEvaluator = new RuleEvaluator(objectMapper);
    }

    @Test
    void testEvaluateSimpleEqualsCondition_True() {
        // Given
        RuleItem condition = new RuleItem("age", "=", 25);
        RuleDefinition rule = new RuleDefinition("and", List.of(condition));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 25);

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateSimpleEqualsCondition_False() {
        // Given
        RuleItem condition = new RuleItem("age", "=", 25);
        RuleDefinition rule = new RuleDefinition("and", List.of(condition));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 30);

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertFalse(result);
    }

    @Test
    void testEvaluateAndConditions_AllTrue() {
        // Given
        RuleItem condition1 = new RuleItem("age", ">=", 18);
        RuleItem condition2 = new RuleItem("status", "=", "active");
        RuleDefinition rule = new RuleDefinition("and", Arrays.asList(condition1, condition2));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 25);
        fieldValues.put("status", "active");

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateAndConditions_OneFalse() {
        // Given
        RuleItem condition1 = new RuleItem("age", ">=", 18);
        RuleItem condition2 = new RuleItem("status", "=", "active");
        RuleDefinition rule = new RuleDefinition("and", Arrays.asList(condition1, condition2));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 16);
        fieldValues.put("status", "active");

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertFalse(result);
    }

    @Test
    void testEvaluateOrConditions_OneTrue() {
        // Given
        RuleItem condition1 = new RuleItem("age", "<", 18);
        RuleItem condition2 = new RuleItem("status", "=", "premium");
        RuleDefinition rule = new RuleDefinition("or", Arrays.asList(condition1, condition2));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 25);
        fieldValues.put("status", "premium");

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateStringContains() {
        // Given
        RuleItem condition = new RuleItem("name", "contains", "john");
        RuleDefinition rule = new RuleDefinition("and", List.of(condition));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("name", "John Doe");

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateNumericComparison() {
        // Given
        RuleItem condition = new RuleItem("score", ">", 80);
        RuleDefinition rule = new RuleDefinition("and", List.of(condition));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("score", 85);

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateWithTrace() {
        // Given
        RuleItem condition = new RuleItem("age", ">=", 18);
        RuleDefinition rule = new RuleDefinition("and", List.of(condition));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("age", 25);

        // When
        RuleExecutionResult result = ruleEvaluator.evaluateWithTrace(rule, fieldValues);

        // Then
        assertTrue(result.isResult());
        assertFalse(result.isHasError());
        assertNotNull(result.getTraces());
        assertFalse(result.getTraces().isEmpty());
        assertTrue(result.getExecutionDurationMs() >= 0);
    }

    @Test
    void testParseRuleDefinition() {
        // Given
        String ruleJson = """
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
            """;

        // When
        RuleDefinition rule = ruleEvaluator.parseRuleDefinition(ruleJson);

        // Then
        assertNotNull(rule);
        assertEquals("and", rule.getCombinator());
        assertNotNull(rule.getRules());
        assertEquals(1, rule.getRules().size());
        
        RuleItem item = rule.getRules().get(0);
        assertEquals("age", item.getField());
        assertEquals(">=", item.getOperator());
        assertEquals(18, item.getValue());
    }

    @Test
    void testParseInvalidRuleDefinition() {
        // Given
        String invalidJson = "{ invalid json }";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            ruleEvaluator.parseRuleDefinition(invalidJson);
        });
    }

    @Test
    void testEvaluateEmptyRule() {
        // Given
        RuleDefinition rule = new RuleDefinition("and", List.of());
        Map<String, Object> fieldValues = new HashMap<>();

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertTrue(result); // Empty rule should evaluate to true
    }

    @Test
    void testEvaluateNullRule() {
        // Given
        Map<String, Object> fieldValues = new HashMap<>();

        // When
        boolean result = ruleEvaluator.evaluateRule(null, fieldValues);

        // Then
        assertFalse(result); // Null rule should evaluate to false due to exception handling
    }

    @Test
    void testEvaluateIsEmptyOperator() {
        // Given
        RuleItem condition = new RuleItem("description", "isEmpty", null);
        RuleDefinition rule = new RuleDefinition("and", List.of(condition));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("description", "");

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateIsNotEmptyOperator() {
        // Given
        RuleItem condition = new RuleItem("description", "isNotEmpty", null);
        RuleDefinition rule = new RuleDefinition("and", List.of(condition));
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("description", "Some text");

        // When
        boolean result = ruleEvaluator.evaluateRule(rule, fieldValues);

        // Then
        assertTrue(result);
    }
}