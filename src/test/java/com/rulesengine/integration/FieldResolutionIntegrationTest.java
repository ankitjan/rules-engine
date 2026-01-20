package com.rulesengine.integration;

import com.rulesengine.dto.ExecutionContext;
import com.rulesengine.dto.FieldResolutionPlan;
import com.rulesengine.dto.FieldResolutionResult;
import com.rulesengine.entity.FieldConfigEntity;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.model.RuleItem;
import com.rulesengine.repository.FieldConfigRepository;
import com.rulesengine.service.FieldResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FieldResolutionIntegrationTest {

    @Autowired
    private FieldResolutionService fieldResolutionService;

    @Autowired
    private FieldConfigRepository fieldConfigRepository;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        executionContext = new ExecutionContext();
        executionContext.setEntityId("integration-test-entity");
        executionContext.setEntityType("TestEntity");
        executionContext.addFieldValue("contextField", "contextValue");
        
        // Clean up any existing test data
        fieldConfigRepository.deleteAll();
        
        // Create test field configurations
        createTestFieldConfigurations();
    }

    @Test
    void testFieldResolution_WithStaticAndDefaultFields() {
        // Given
        List<String> fieldNames = Arrays.asList("staticField", "defaultField", "contextField");

        // When
        Map<String, Object> resolvedValues = fieldResolutionService.resolveFields(fieldNames, executionContext);

        // Then
        assertNotNull(resolvedValues);
        assertEquals("defaultStaticValue", resolvedValues.get("staticField"));
        assertEquals("defaultValue", resolvedValues.get("defaultField"));
        assertEquals("contextValue", resolvedValues.get("contextField"));
    }

    @Test
    void testCreateResolutionPlan_Integration() {
        // Given
        List<String> fieldNames = Arrays.asList("staticField", "defaultField", "calculatedField");

        // When
        FieldResolutionPlan plan = fieldResolutionService.createResolutionPlan(fieldNames);

        // Then
        assertNotNull(plan);
        assertNotNull(plan.getStaticFieldValues());
        assertTrue(plan.getStaticFieldValues().containsKey("staticField"));
        assertTrue(plan.getStaticFieldValues().containsKey("defaultField"));
        assertTrue(plan.getEstimatedExecutionTimeMs() >= 0);
        
        // Should have sequential execution for calculated fields
        if (plan.hasSequentialExecution()) {
            assertFalse(plan.getSequentialChains().isEmpty());
        }
    }

    @Test
    void testExtractFieldNamesFromRule_Integration() {
        // Given
        RuleDefinition ruleDefinition = createTestRuleDefinition();

        // When
        List<String> extractedFields = fieldResolutionService.extractFieldNamesFromRule(ruleDefinition);

        // Then
        assertNotNull(extractedFields);
        assertFalse(extractedFields.isEmpty());
        assertTrue(extractedFields.contains("staticField"));
        assertTrue(extractedFields.contains("defaultField"));
    }

    @Test
    void testFieldResolution_WithNonExistentFields() {
        // Given
        List<String> fieldNames = Arrays.asList("nonExistentField1", "nonExistentField2");

        // When
        Map<String, Object> resolvedValues = fieldResolutionService.resolveFields(fieldNames, executionContext);

        // Then
        assertNotNull(resolvedValues);
        // Should still contain context values
        assertEquals("contextValue", resolvedValues.get("contextField"));
    }

    @Test
    void testExecuteResolutionPlan_WithStaticFieldsOnly() {
        // Given
        List<String> fieldNames = Arrays.asList("staticField", "defaultField");
        FieldResolutionPlan plan = fieldResolutionService.createResolutionPlan(fieldNames);

        // When
        FieldResolutionResult result = fieldResolutionService.executeResolutionPlan(plan, executionContext, "test-cache-key");

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getOverallStatus());
        assertFalse(result.isHasErrors());
        assertTrue(result.getSuccessfulFields() > 0);
        assertEquals(0, result.getFailedFields());
        assertTrue(result.getTotalExecutionTimeMs() >= 0);
        
        // Check resolved values
        assertTrue(result.hasResolvedValue("staticField"));
        assertTrue(result.hasResolvedValue("defaultField"));
        assertTrue(result.hasResolvedValue("contextField"));
    }

    @Test
    void testFieldResolution_Performance() {
        // Given
        List<String> fieldNames = Arrays.asList("staticField", "defaultField", "calculatedField");
        
        // When
        long startTime = System.currentTimeMillis();
        Map<String, Object> resolvedValues = fieldResolutionService.resolveFields(fieldNames, executionContext);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertNotNull(resolvedValues);
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 1000, "Field resolution should complete within 1 second for static fields");
    }

    private void createTestFieldConfigurations() {
        // Static field with default value
        FieldConfigEntity staticField = new FieldConfigEntity("staticField", "STRING", "Static field for testing");
        staticField.setDefaultValue("defaultStaticValue");
        staticField.setIsRequired(false);
        fieldConfigRepository.save(staticField);

        // Field with default value
        FieldConfigEntity defaultField = new FieldConfigEntity("defaultField", "STRING", "Field with default value");
        defaultField.setDefaultValue("defaultValue");
        defaultField.setIsRequired(false);
        fieldConfigRepository.save(defaultField);

        // Calculated field (for future testing)
        FieldConfigEntity calculatedField = new FieldConfigEntity("calculatedField", "NUMBER", "Calculated field");
        calculatedField.setIsCalculated(true);
        calculatedField.setCalculatorConfigJson("{\"expression\":\"staticField.length() + 10\"}");
        calculatedField.setDependenciesJson("[\"staticField\"]");
        calculatedField.setIsRequired(false);
        fieldConfigRepository.save(calculatedField);

        // Required field with default
        FieldConfigEntity requiredField = new FieldConfigEntity("requiredField", "STRING", "Required field");
        requiredField.setDefaultValue("requiredDefaultValue");
        requiredField.setIsRequired(true);
        fieldConfigRepository.save(requiredField);
    }

    private RuleDefinition createTestRuleDefinition() {
        RuleDefinition ruleDefinition = new RuleDefinition();
        
        // Create condition for staticField
        RuleItem condition1 = new RuleItem();
        condition1.setField("staticField");
        condition1.setOperator("equals");
        condition1.setValue("testValue");
        
        // Create condition for defaultField
        RuleItem condition2 = new RuleItem();
        condition2.setField("defaultField");
        condition2.setOperator("not_empty");
        
        // Create group with AND combinator
        RuleItem group = new RuleItem();
        group.setCombinator("AND");
        group.setRules(Arrays.asList(condition1, condition2));
        
        ruleDefinition.setRules(Arrays.asList(group));
        ruleDefinition.setCombinator("AND");
        
        return ruleDefinition;
    }
}