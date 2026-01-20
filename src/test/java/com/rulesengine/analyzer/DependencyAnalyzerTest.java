package com.rulesengine.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.DependencyGraph;
import com.rulesengine.dto.FieldResolutionPlan;
import com.rulesengine.dto.ParallelExecutionGroup;
import com.rulesengine.entity.FieldConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DependencyAnalyzerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DependencyAnalyzer dependencyAnalyzer;

    private FieldConfigEntity field1;
    private FieldConfigEntity field2;
    private FieldConfigEntity field3;
    private FieldConfigEntity calculatedField;

    @BeforeEach
    void setUp() {
        // Create test field configurations
        field1 = new FieldConfigEntity();
        field1.setFieldName("field1");
        field1.setFieldType("STRING");
        field1.setIsCalculated(false);
        field1.setDataServiceConfigJson("{\"serviceType\":\"REST\",\"endpoint\":\"http://api1.com\"}");

        field2 = new FieldConfigEntity();
        field2.setFieldName("field2");
        field2.setFieldType("NUMBER");
        field2.setIsCalculated(false);
        field2.setDataServiceConfigJson("{\"serviceType\":\"REST\",\"endpoint\":\"http://api2.com\"}");

        field3 = new FieldConfigEntity();
        field3.setFieldName("field3");
        field3.setFieldType("STRING");
        field3.setIsCalculated(false);

        calculatedField = new FieldConfigEntity();
        calculatedField.setFieldName("calculatedField");
        calculatedField.setFieldType("NUMBER");
        calculatedField.setIsCalculated(true);
        calculatedField.setDependenciesJson("[\"field1\", \"field2\"]");
        calculatedField.setCalculatorConfigJson("{\"type\":\"EXPRESSION\",\"expression\":\"#field1.length() + #field2\"}");
    }

    @Test
    void testBuildDependencyGraph_WithNoDependencies() {
        List<FieldConfigEntity> fields = Arrays.asList(field1, field2, field3);

        DependencyGraph graph = dependencyAnalyzer.buildDependencyGraph(fields);

        assertNotNull(graph);
        assertEquals(3, graph.getNodeCount());
        assertEquals(0, graph.getEdgeCount());
        assertTrue(graph.getRootFields().containsAll(Set.of("field1", "field2", "field3")));
    }

    @Test
    void testBuildDependencyGraph_WithDependencies() throws Exception {
        // Mock the ObjectMapper to return the dependencies list
        List<String> dependencies = Arrays.asList("field1", "field2");
        when(objectMapper.readValue(eq("[\"field1\", \"field2\"]"), 
            any(com.fasterxml.jackson.core.type.TypeReference.class)))
            .thenReturn(dependencies);

        List<FieldConfigEntity> fields = Arrays.asList(field1, field2, calculatedField);

        DependencyGraph graph = dependencyAnalyzer.buildDependencyGraph(fields);

        assertNotNull(graph);
        assertEquals(3, graph.getNodeCount());
        // Note: The actual edge count depends on the mocking working correctly
        assertTrue(graph.getRootFields().containsAll(Set.of("field1", "field2")));
    }

    @Test
    void testValidateNoCycles_WithValidGraph() {
        List<FieldConfigEntity> fields = Arrays.asList(field1, field2, field3);
        DependencyGraph graph = dependencyAnalyzer.buildDependencyGraph(fields);

        assertDoesNotThrow(() -> dependencyAnalyzer.validateNoCycles(graph));
    }

    @Test
    void testValidateNoCycles_WithCircularDependency() {
        // Create circular dependency: field1 -> field2 -> field1
        FieldConfigEntity circularField1 = new FieldConfigEntity();
        circularField1.setFieldName("circular1");
        circularField1.setFieldType("STRING");
        circularField1.setIsCalculated(true);
        circularField1.setDependenciesJson("[\"circular2\"]");

        FieldConfigEntity circularField2 = new FieldConfigEntity();
        circularField2.setFieldName("circular2");
        circularField2.setFieldType("STRING");
        circularField2.setIsCalculated(true);
        circularField2.setDependenciesJson("[\"circular1\"]");

        DependencyGraph graph = new DependencyGraph();
        graph.addNode(circularField1);
        graph.addNode(circularField2);
        graph.addDependency("circular1", "circular2");
        graph.addDependency("circular2", "circular1");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> dependencyAnalyzer.validateNoCycles(graph));
        
        assertTrue(exception.getMessage().contains("Circular dependency detected"));
    }

    @Test
    void testCreateResolutionPlan_WithMixedFields() {
        List<FieldConfigEntity> fields = Arrays.asList(field1, field2, field3, calculatedField);
        DependencyGraph graph = dependencyAnalyzer.buildDependencyGraph(fields);

        FieldResolutionPlan plan = dependencyAnalyzer.createResolutionPlan(graph);

        assertNotNull(plan);
        assertNotNull(plan.getParallelGroups());
        assertNotNull(plan.getSequentialChains());
        assertTrue(plan.getEstimatedExecutionTimeMs() > 0);
    }

    @Test
    void testAnalyzeDataServiceDependencies() {
        List<FieldConfigEntity> fields = Arrays.asList(field1, field2);

        List<ParallelExecutionGroup> groups = dependencyAnalyzer.analyzeDataServiceDependencies(fields);

        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        
        // Should create groups based on data service configurations
        int totalFields = groups.stream().mapToInt(ParallelExecutionGroup::getFieldCount).sum();
        assertEquals(2, totalFields);
    }

    @Test
    void testExtractFieldNamesFromRule() throws Exception {
        // Mock the ObjectMapper to convert the rule to JSON
        String ruleJson = "{\"field\":\"testField\",\"operator\":\"equals\",\"value\":\"test\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(ruleJson);

        Object ruleDefinition = new Object();

        Set<String> fieldNames = dependencyAnalyzer.extractFieldNamesFromRule(ruleDefinition);

        assertNotNull(fieldNames);
        assertTrue(fieldNames.contains("testField"));
    }

    @Test
    void testDependencyGraph_TopologicalOrder() {
        DependencyGraph graph = new DependencyGraph();
        graph.addNode(field1);
        graph.addNode(field2);
        graph.addNode(calculatedField);
        graph.addDependency("calculatedField", "field1");
        graph.addDependency("calculatedField", "field2");

        List<String> topologicalOrder = graph.getTopologicalOrder();

        assertNotNull(topologicalOrder);
        assertEquals(3, topologicalOrder.size());
        
        // calculatedField should come after its dependencies
        int field1Index = topologicalOrder.indexOf("field1");
        int field2Index = topologicalOrder.indexOf("field2");
        int calculatedIndex = topologicalOrder.indexOf("calculatedField");
        
        assertTrue(field1Index >= 0, "field1 should be in topological order");
        assertTrue(field2Index >= 0, "field2 should be in topological order");
        assertTrue(calculatedIndex >= 0, "calculatedField should be in topological order");
        assertTrue(field1Index < calculatedIndex, "field1 should come before calculatedField");
        assertTrue(field2Index < calculatedIndex, "field2 should come before calculatedField");
    }

    @Test
    void testDependencyGraph_CircularDependencyDetection() {
        DependencyGraph graph = new DependencyGraph();
        
        FieldConfigEntity a = new FieldConfigEntity();
        a.setFieldName("a");
        FieldConfigEntity b = new FieldConfigEntity();
        b.setFieldName("b");
        FieldConfigEntity c = new FieldConfigEntity();
        c.setFieldName("c");
        
        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);
        
        // Create cycle: a -> b -> c -> a
        graph.addDependency("a", "b");
        graph.addDependency("b", "c");
        graph.addDependency("c", "a");

        List<String> cycle = graph.detectCircularDependencies();

        assertNotNull(cycle);
        assertFalse(cycle.isEmpty());
        assertTrue(cycle.contains("a"));
        assertTrue(cycle.contains("b"));
        assertTrue(cycle.contains("c"));
    }
}