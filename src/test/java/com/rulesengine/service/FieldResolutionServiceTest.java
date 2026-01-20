package com.rulesengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.analyzer.DependencyAnalyzer;
import com.rulesengine.client.config.RestServiceConfig;
import com.rulesengine.dto.*;
import com.rulesengine.entity.FieldConfigEntity;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.model.RuleItem;
import com.rulesengine.repository.FieldConfigRepository;
import com.rulesengine.service.DataServiceClient;
import com.rulesengine.service.FieldCalculatorService;
import com.rulesengine.service.FieldMappingService;
import com.rulesengine.service.FieldResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FieldResolutionServiceTest {

    @Mock
    private FieldConfigRepository fieldConfigRepository;

    @Mock
    private DataServiceClient dataServiceClient;

    @Mock
    private FieldMappingService fieldMappingService;
    
    @Mock
    private FieldCalculatorService fieldCalculatorService;
    
    @Mock
    private DependencyAnalyzer dependencyAnalyzer;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FieldResolutionService fieldResolutionService;

    private ExecutionContext executionContext;
    private List<FieldConfigEntity> fieldConfigs;

    @BeforeEach
    void setUp() {
        executionContext = new ExecutionContext();
        executionContext.setEntityId("test-entity-123");
        executionContext.setEntityType("TestEntity");
        executionContext.addFieldValue("staticField", "staticValue");

        fieldConfigs = createTestFieldConfigs();
        
        // Mock dependency analyzer behavior with proper plans
        DependencyGraph mockGraph = new DependencyGraph();
        FieldResolutionPlan mockPlan = new FieldResolutionPlan();
        
        // Create test field entities for the mock plan
        FieldConfigEntity dataServiceField = new FieldConfigEntity("dataServiceField", "STRING", "Data service field");
        dataServiceField.setDataServiceConfigJson("{\"serviceType\":\"REST\"}");
        
        FieldConfigEntity calculatedField = new FieldConfigEntity("calculatedField", "NUMBER", "Calculated field");
        calculatedField.setIsCalculated(true);
        
        // Set up mock plan with parallel and sequential execution
        ParallelExecutionGroup parallelGroup = new ParallelExecutionGroup();
        parallelGroup.addField(dataServiceField);
        
        SequentialExecutionChain sequentialChain = new SequentialExecutionChain();
        sequentialChain.addField(calculatedField);
        
        // Use setters instead of add methods
        mockPlan.setParallelGroups(List.of(parallelGroup));
        mockPlan.setSequentialChains(List.of(sequentialChain));
        mockPlan.setEstimatedExecutionTimeMs(100L);
        
        lenient().when(dependencyAnalyzer.buildDependencyGraph(any())).thenReturn(mockGraph);
        lenient().when(dependencyAnalyzer.createResolutionPlan(any())).thenReturn(mockPlan);
    }

    @Test
    void testResolveFields_WithStaticFields() {
        // Given
        List<String> fieldNames = Arrays.asList("staticField", "defaultField");
        
        FieldConfigEntity staticField = new FieldConfigEntity("staticField", "STRING", "Static field");
        FieldConfigEntity defaultField = new FieldConfigEntity("defaultField", "STRING", "Default field");
        defaultField.setDefaultValue("defaultValue");
        
        when(fieldConfigRepository.findByFieldNamesActive(fieldNames))
            .thenReturn(Arrays.asList(staticField, defaultField));

        // When
        Map<String, Object> result = fieldResolutionService.resolveFields(fieldNames, executionContext);

        // Then
        assertNotNull(result);
        assertEquals("staticValue", result.get("staticField"));
        assertEquals("defaultValue", result.get("defaultField"));
    }

    @Test
    void testResolveFields_WithDataServiceFields() throws Exception {
        // Given
        List<String> fieldNames = Arrays.asList("dataServiceField");
        
        FieldConfigEntity dataServiceField = new FieldConfigEntity("dataServiceField", "STRING", "Data service field");
        dataServiceField.setDataServiceConfigJson("{\"serviceType\":\"REST\",\"endpoint\":\"http://test.com\"}");
        dataServiceField.setMapperExpression("data.value");
        
        when(fieldConfigRepository.findByFieldNamesActive(fieldNames))
            .thenReturn(Arrays.asList(dataServiceField));
        
        // Mock ObjectMapper calls for parseDataServiceConfig
        Map<String, Object> serviceConfig = new HashMap<>();
        serviceConfig.put("serviceType", "REST");
        serviceConfig.put("endpoint", "http://test.com");
        lenient().when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
            .thenReturn(serviceConfig);
        
        // Mock the RestServiceConfig deserialization
        RestServiceConfig restConfig = new RestServiceConfig();
        restConfig.setEndpoint("http://test.com");
        lenient().when(objectMapper.readValue(anyString(), eq(RestServiceConfig.class)))
            .thenReturn(restConfig);
        
        Object mockResponse = Map.of("data", Map.of("value", "resolvedValue"));
        lenient().when(dataServiceClient.executeRequest(any(), any())).thenReturn(mockResponse);
        lenient().when(fieldMappingService.extractFieldValue(any(), any()))
            .thenReturn("resolvedValue");

        // When
        Map<String, Object> result = fieldResolutionService.resolveFields(fieldNames, executionContext);

        // Then
        assertNotNull(result);
        assertEquals("resolvedValue", result.get("dataServiceField"));
    }

    @Test
    void testCreateResolutionPlan_WithMixedFields() {
        // Given
        List<String> fieldNames = Arrays.asList("staticField", "dataServiceField", "calculatedField");
        
        FieldConfigEntity staticField = new FieldConfigEntity("staticField", "STRING", "Static field");
        staticField.setDefaultValue("staticValue");
        
        FieldConfigEntity dataServiceField = new FieldConfigEntity("dataServiceField", "STRING", "Data service field");
        dataServiceField.setDataServiceConfigJson("{\"serviceType\":\"REST\"}");
        
        FieldConfigEntity calculatedField = new FieldConfigEntity("calculatedField", "NUMBER", "Calculated field");
        calculatedField.setIsCalculated(true);
        calculatedField.setCalculatorConfigJson("{\"expression\":\"field1 + field2\"}");
        
        when(fieldConfigRepository.findByFieldNamesActive(fieldNames))
            .thenReturn(Arrays.asList(staticField, dataServiceField, calculatedField));

        // When
        FieldResolutionPlan plan = fieldResolutionService.createResolutionPlan(fieldNames);

        // Then
        assertNotNull(plan);
        assertNotNull(plan.getStaticFieldValues());
        assertEquals("staticValue", plan.getStaticFieldValues().get("staticField"));
        assertTrue(plan.hasParallelExecution());
        assertTrue(plan.hasSequentialExecution());
        assertTrue(plan.getEstimatedExecutionTimeMs() > 0);
    }

    @Test
    void testExtractFieldNamesFromRule() {
        // Given
        RuleDefinition ruleDefinition = new RuleDefinition();
        
        RuleItem condition1 = new RuleItem();
        condition1.setField("field1");
        condition1.setOperator("equals");
        condition1.setValue("value1");
        
        RuleItem condition2 = new RuleItem();
        condition2.setField("field2");
        condition2.setOperator("greater_than");
        condition2.setValue("100");
        
        RuleItem group = new RuleItem();
        group.setCombinator("AND");
        group.setRules(Arrays.asList(condition1, condition2));
        
        ruleDefinition.setRules(Arrays.asList(group));

        // When
        List<String> fieldNames = fieldResolutionService.extractFieldNamesFromRule(ruleDefinition);

        // Then
        assertNotNull(fieldNames);
        assertEquals(2, fieldNames.size());
        assertTrue(fieldNames.contains("field1"));
        assertTrue(fieldNames.contains("field2"));
    }

    @Test
    void testResolveFields_WithError_FallsBackToContext() {
        // Given
        List<String> fieldNames = Arrays.asList("errorField");
        
        when(fieldConfigRepository.findByFieldNamesActive(fieldNames))
            .thenThrow(new RuntimeException("Database error"));

        // When
        Map<String, Object> result = fieldResolutionService.resolveFields(fieldNames, executionContext);

        // Then
        assertNotNull(result);
        // When there's an error in field resolution, it should return the context field values as fallback
        // The context contains "staticField" -> "staticValue"
        assertEquals("staticValue", result.get("staticField"));
        // The errorField should not be present since it couldn't be resolved and isn't in context
        assertNull(result.get("errorField"));
    }

    @Test
    void testExecuteResolutionPlan_WithStaticValues() {
        // Given
        FieldResolutionPlan plan = new FieldResolutionPlan();
        Map<String, Object> staticValues = new HashMap<>();
        staticValues.put("staticField1", "value1");
        staticValues.put("staticField2", "value2");
        plan.setStaticFieldValues(staticValues);

        // When
        FieldResolutionResult result = fieldResolutionService.executeResolutionPlan(plan, executionContext, "test-cache-key");

        // Then
        assertNotNull(result);
        assertEquals("SUCCESS", result.getOverallStatus());
        assertEquals("value1", result.getResolvedValue("staticField1"));
        assertEquals("value2", result.getResolvedValue("staticField2"));
        assertEquals("staticValue", result.getResolvedValue("staticField")); // From context
        assertTrue(result.getTotalExecutionTimeMs() >= 0);
    }

    @Test
    void testCreateResolutionPlan_EmptyFieldList() {
        // Given
        List<String> fieldNames = new ArrayList<>();
        when(fieldConfigRepository.findByFieldNamesActive(fieldNames))
            .thenReturn(new ArrayList<>());
        
        // Override the mock for this test to return an empty plan
        DependencyGraph emptyGraph = new DependencyGraph();
        FieldResolutionPlan emptyPlan = new FieldResolutionPlan();
        when(dependencyAnalyzer.buildDependencyGraph(any())).thenReturn(emptyGraph);
        when(dependencyAnalyzer.createResolutionPlan(any())).thenReturn(emptyPlan);

        // When
        FieldResolutionPlan plan = fieldResolutionService.createResolutionPlan(fieldNames);

        // Then
        assertNotNull(plan);
        assertFalse(plan.hasParallelExecution());
        assertFalse(plan.hasSequentialExecution());
        assertEquals(0, plan.getTotalFieldCount());
        assertEquals(0, plan.getTotalServiceCount());
    }

    private List<FieldConfigEntity> createTestFieldConfigs() {
        List<FieldConfigEntity> configs = new ArrayList<>();
        
        // Static field with default value
        FieldConfigEntity staticField = new FieldConfigEntity("staticField", "STRING", "Static field");
        staticField.setDefaultValue("defaultStaticValue");
        configs.add(staticField);
        
        // Data service field
        FieldConfigEntity dataServiceField = new FieldConfigEntity("dataServiceField", "STRING", "Data service field");
        dataServiceField.setDataServiceConfigJson("{\"serviceType\":\"REST\",\"endpoint\":\"http://test.com\"}");
        dataServiceField.setMapperExpression("data.value");
        configs.add(dataServiceField);
        
        // Calculated field
        FieldConfigEntity calculatedField = new FieldConfigEntity("calculatedField", "NUMBER", "Calculated field");
        calculatedField.setIsCalculated(true);
        calculatedField.setCalculatorConfigJson("{\"expression\":\"field1 + field2\"}");
        calculatedField.setDependenciesJson("[\"field1\", \"field2\"]");
        configs.add(calculatedField);
        
        return configs;
    }
}