package com.rulesengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.analyzer.DependencyAnalyzer;
import com.rulesengine.calculator.CalculatorException;
import com.rulesengine.calculator.CalculatorRegistry;
import com.rulesengine.calculator.CustomCalculatorLoader;
import com.rulesengine.calculator.FieldCalculator;
import com.rulesengine.calculator.builtin.*;
import com.rulesengine.dto.DependencyGraph;
import com.rulesengine.entity.FieldConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldCalculatorServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DependencyAnalyzer dependencyAnalyzer;

    @Mock
    private CalculatorRegistry calculatorRegistry;

    @Mock
    private CustomCalculatorLoader customCalculatorLoader;

    @InjectMocks
    private FieldCalculatorService fieldCalculatorService;

    private FieldConfigEntity expressionField;
    private FieldConfigEntity builtinField;
    private Map<String, Object> baseFieldValues;

    @BeforeEach
    void setUp() {
        // Create expression-based calculated field
        expressionField = new FieldConfigEntity();
        expressionField.setFieldName("totalAmount");
        expressionField.setFieldType("NUMBER");
        expressionField.setIsCalculated(true);
        expressionField.setCalculatorConfigJson("{\"type\":\"EXPRESSION\",\"expression\":\"#price * #quantity\"}");
        expressionField.setDependenciesJson("[\"price\", \"quantity\"]");

        // Create builtin function calculated field
        builtinField = new FieldConfigEntity();
        builtinField.setFieldName("averageScore");
        builtinField.setFieldType("NUMBER");
        builtinField.setIsCalculated(true);
        builtinField.setCalculatorConfigJson("{\"type\":\"BUILTIN\",\"function\":\"average\",\"parameters\":{\"fields\":[\"score1\",\"score2\",\"score3\"]}}");
        builtinField.setDependenciesJson("[\"score1\", \"score2\", \"score3\"]");

        // Base field values
        baseFieldValues = new HashMap<>();
        baseFieldValues.put("price", 10.0);
        baseFieldValues.put("quantity", 5);
        baseFieldValues.put("score1", 85.0);
        baseFieldValues.put("score2", 92.0);
        baseFieldValues.put("score3", 78.0);
    }

    @Test
    void testCalculateFields_WithExpressionCalculator() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("EXPRESSION");
        config.setExpression("#price * #quantity");

        when(objectMapper.readValue(eq(expressionField.getCalculatorConfigJson()), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        // Mock the dependency analyzer to return a simple graph
        DependencyGraph mockGraph = new DependencyGraph();
        mockGraph.addNode(expressionField);
        when(dependencyAnalyzer.buildDependencyGraph(any())).thenReturn(mockGraph);

        List<FieldConfigEntity> calculatedFields = Arrays.asList(expressionField);

        Map<String, Object> results = fieldCalculatorService.calculateFields(calculatedFields, baseFieldValues);

        assertNotNull(results);
        assertTrue(results.containsKey("totalAmount"));
        assertEquals(50.0, results.get("totalAmount"));
        
        // Should also contain original base values
        assertEquals(10.0, results.get("price"));
        assertEquals(5, results.get("quantity"));
    }

    @Test
    void testCalculateFields_WithBuiltinCalculator() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("BUILTIN");
        config.setFunction("average");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fields", Arrays.asList("score1", "score2", "score3"));
        config.setParameters(parameters);

        when(objectMapper.readValue(eq(builtinField.getCalculatorConfigJson()), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        // Mock the calculator registry
        AverageCalculator averageCalculator = new AverageCalculator();
        when(calculatorRegistry.getCalculator("average")).thenReturn(averageCalculator);

        // Mock the dependency analyzer to return a simple graph
        DependencyGraph mockGraph = new DependencyGraph();
        mockGraph.addNode(builtinField);
        when(dependencyAnalyzer.buildDependencyGraph(any())).thenReturn(mockGraph);

        List<FieldConfigEntity> calculatedFields = Arrays.asList(builtinField);

        Map<String, Object> results = fieldCalculatorService.calculateFields(calculatedFields, baseFieldValues);

        assertNotNull(results);
        assertTrue(results.containsKey("averageScore"));
        assertEquals(85.0, results.get("averageScore")); // (85 + 92 + 78) / 3 = 85
    }

    @Test
    void testCalculateField_ExpressionCalculator() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("EXPRESSION");
        config.setExpression("#price * #quantity");

        when(objectMapper.readValue(eq(expressionField.getCalculatorConfigJson()), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        Object result = fieldCalculatorService.calculateField(expressionField, baseFieldValues);

        assertNotNull(result);
        assertEquals(50.0, result);
    }

    @Test
    void testCalculateField_BuiltinSum() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("BUILTIN");
        config.setFunction("sum");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fields", Arrays.asList("score1", "score2", "score3"));
        config.setParameters(parameters);

        when(objectMapper.readValue(any(String.class), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        // Mock the calculator registry
        SumCalculator sumCalculator = new SumCalculator();
        when(calculatorRegistry.getCalculator("sum")).thenReturn(sumCalculator);

        FieldConfigEntity sumField = new FieldConfigEntity();
        sumField.setFieldName("totalScore");
        sumField.setIsCalculated(true);
        sumField.setCalculatorConfigJson("{\"type\":\"BUILTIN\",\"function\":\"sum\"}");

        Object result = fieldCalculatorService.calculateField(sumField, baseFieldValues);

        assertNotNull(result);
        assertEquals(255.0, result); // 85 + 92 + 78 = 255
    }

    @Test
    void testCalculateField_BuiltinMax() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("BUILTIN");
        config.setFunction("max");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fields", Arrays.asList("score1", "score2", "score3"));
        config.setParameters(parameters);

        when(objectMapper.readValue(any(String.class), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        // Mock the calculator registry
        MaxCalculator maxCalculator = new MaxCalculator();
        when(calculatorRegistry.getCalculator("max")).thenReturn(maxCalculator);

        FieldConfigEntity maxField = new FieldConfigEntity();
        maxField.setFieldName("maxScore");
        maxField.setIsCalculated(true);
        maxField.setCalculatorConfigJson("{\"type\":\"BUILTIN\",\"function\":\"max\"}");

        Object result = fieldCalculatorService.calculateField(maxField, baseFieldValues);

        assertNotNull(result);
        assertEquals(92.0, result);
    }

    @Test
    void testCalculateField_BuiltinMin() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("BUILTIN");
        config.setFunction("min");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fields", Arrays.asList("score1", "score2", "score3"));
        config.setParameters(parameters);

        when(objectMapper.readValue(any(String.class), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        // Mock the calculator registry
        MinCalculator minCalculator = new MinCalculator();
        when(calculatorRegistry.getCalculator("min")).thenReturn(minCalculator);

        FieldConfigEntity minField = new FieldConfigEntity();
        minField.setFieldName("minScore");
        minField.setIsCalculated(true);
        minField.setCalculatorConfigJson("{\"type\":\"BUILTIN\",\"function\":\"min\"}");

        Object result = fieldCalculatorService.calculateField(minField, baseFieldValues);

        assertNotNull(result);
        assertEquals(78.0, result);
    }

    @Test
    void testCalculateField_BuiltinCount() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("BUILTIN");
        config.setFunction("count");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fields", Arrays.asList("score1", "score2", "score3"));
        config.setParameters(parameters);

        when(objectMapper.readValue(any(String.class), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        // Mock the calculator registry
        CountCalculator countCalculator = new CountCalculator();
        when(calculatorRegistry.getCalculator("count")).thenReturn(countCalculator);

        FieldConfigEntity countField = new FieldConfigEntity();
        countField.setFieldName("scoreCount");
        countField.setIsCalculated(true);
        countField.setCalculatorConfigJson("{\"type\":\"BUILTIN\",\"function\":\"count\"}");

        Object result = fieldCalculatorService.calculateField(countField, baseFieldValues);

        assertNotNull(result);
        assertEquals(3, result);
    }

    @Test
    void testCalculateField_BuiltinConcat() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("BUILTIN");
        config.setFunction("concat");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("fields", Arrays.asList("firstName", "lastName"));
        parameters.put("separator", " ");
        config.setParameters(parameters);

        when(objectMapper.readValue(any(String.class), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        // Mock the calculator registry
        ConcatCalculator concatCalculator = new ConcatCalculator();
        when(calculatorRegistry.getCalculator("concat")).thenReturn(concatCalculator);

        FieldConfigEntity concatField = new FieldConfigEntity();
        concatField.setFieldName("fullName");
        concatField.setIsCalculated(true);
        concatField.setCalculatorConfigJson("{\"type\":\"BUILTIN\",\"function\":\"concat\"}");

        Map<String, Object> values = new HashMap<>();
        values.put("firstName", "John");
        values.put("lastName", "Doe");

        Object result = fieldCalculatorService.calculateField(concatField, values);

        assertNotNull(result);
        assertEquals("John Doe", result);
    }

    @Test
    void testCalculateField_InvalidCalculatorType() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("INVALID");

        when(objectMapper.readValue(any(String.class), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        FieldConfigEntity invalidField = new FieldConfigEntity();
        invalidField.setFieldName("invalid");
        invalidField.setIsCalculated(true);
        invalidField.setCalculatorConfigJson("{\"type\":\"INVALID\"}");

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> fieldCalculatorService.calculateField(invalidField, baseFieldValues));
        
        assertTrue(exception.getMessage().contains("Field calculation failed"));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void testCalculateField_CustomCalculator() throws Exception {
        FieldCalculatorService.CalculatorConfig config = new FieldCalculatorService.CalculatorConfig();
        config.setType("CUSTOM");
        config.setCalculatorClass("com.example.CustomCalculator");
        Map<String, Object> parameters = new HashMap<>();
        config.setParameters(parameters);

        when(objectMapper.readValue(any(String.class), 
            eq(FieldCalculatorService.CalculatorConfig.class))).thenReturn(config);

        // Mock the custom calculator loader to throw exception
        when(customCalculatorLoader.executeCustomCalculator(eq("com.example.CustomCalculator"), 
            eq(parameters), eq(baseFieldValues)))
            .thenThrow(new CalculatorException("Custom calculator not found"));

        FieldConfigEntity customField = new FieldConfigEntity();
        customField.setFieldName("custom");
        customField.setIsCalculated(true);
        customField.setCalculatorConfigJson("{\"type\":\"CUSTOM\"}");

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> fieldCalculatorService.calculateField(customField, baseFieldValues));
        
        assertTrue(exception.getMessage().contains("Field calculation failed"));
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    void testValidateCalculatorExpression_ValidExpression() {
        assertDoesNotThrow(() -> fieldCalculatorService.validateCalculatorExpression("#field1 + #field2"));
    }

    @Test
    void testValidateCalculatorExpression_InvalidExpression() {
        assertThrows(IllegalArgumentException.class, 
            () -> fieldCalculatorService.validateCalculatorExpression("#field1 +"));
    }

    @Test
    void testValidateCalculatorExpression_EmptyExpression() {
        assertThrows(IllegalArgumentException.class, 
            () -> fieldCalculatorService.validateCalculatorExpression(""));
    }

    @Test
    void testValidateCalculatorExpression_NullExpression() {
        assertThrows(IllegalArgumentException.class, 
            () -> fieldCalculatorService.validateCalculatorExpression(null));
    }
}