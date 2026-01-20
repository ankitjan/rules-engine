package com.rulesengine.analyzer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.client.config.DataServiceConfig;
import com.rulesengine.dto.*;
import com.rulesengine.entity.FieldConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes field dependencies and creates optimal resolution plans
 */
@Component
public class DependencyAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalyzer.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Build a dependency graph from a list of field configurations
     */
    public DependencyGraph buildDependencyGraph(List<FieldConfigEntity> fields) {
        logger.debug("Building dependency graph for {} fields", fields.size());
        
        DependencyGraph graph = new DependencyGraph();
        
        // Add all fields as nodes first
        for (FieldConfigEntity field : fields) {
            graph.addNode(field);
        }
        
        // Add dependency edges
        for (FieldConfigEntity field : fields) {
            if (field.hasDependencies()) {
                try {
                    List<String> dependencies = parseDependencies(field.getDependenciesJson());
                    if (dependencies != null) {
                        for (String dependency : dependencies) {
                            // Validate that the dependency field exists
                            if (graph.containsField(dependency)) {
                                graph.addDependency(field.getFieldName(), dependency);
                            } else {
                                logger.warn("Field '{}' depends on '{}' which is not in the field list", 
                                          field.getFieldName(), dependency);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error parsing dependencies for field '{}': {}", 
                               field.getFieldName(), e.getMessage());
                }
            }
        }
        
        logger.debug("Built dependency graph with {} nodes and {} edges", 
                    graph.getNodeCount(), graph.getEdgeCount());
        
        return graph;
    }
    
    /**
     * Create a field resolution plan from a dependency graph with enhanced calculated field support
     */
    public FieldResolutionPlan createResolutionPlan(DependencyGraph graph) {
        logger.debug("Creating enhanced resolution plan for dependency graph");
        
        // First validate no circular dependencies
        validateNoCycles(graph);
        
        FieldResolutionPlan plan = new FieldResolutionPlan();
        
        // Separate fields by their characteristics
        Map<String, FieldConfigEntity> calculatedFields = new HashMap<>();
        Map<String, FieldConfigEntity> dataServiceFields = new HashMap<>();
        Map<String, FieldConfigEntity> staticFields = new HashMap<>();
        
        for (String fieldName : graph.getAllFieldNames()) {
            FieldConfigEntity field = graph.getField(fieldName);
            if (field.getIsCalculated()) {
                calculatedFields.put(fieldName, field);
            } else if (field.hasDataService()) {
                dataServiceFields.put(fieldName, field);
            } else {
                staticFields.put(fieldName, field);
            }
        }
        
        // Create execution groups for data service fields
        List<ParallelExecutionGroup> parallelGroups = groupIndependentServices(
            dataServiceFields, graph);
        List<SequentialExecutionChain> sequentialChains = orderDependentServices(
            dataServiceFields, graph);
        
        plan.setParallelGroups(parallelGroups);
        plan.setSequentialChains(sequentialChains);
        
        // Enhanced calculated field planning
        if (!calculatedFields.isEmpty()) {
            enhanceWithCalculatedFields(plan, calculatedFields, graph);
        }
        
        // Calculate estimated execution time
        long estimatedTime = calculateEstimatedExecutionTime(parallelGroups, sequentialChains);
        plan.setEstimatedExecutionTimeMs(estimatedTime);
        
        logger.debug("Created enhanced resolution plan with {} parallel groups, {} sequential chains, and {} calculated fields", 
                    parallelGroups.size(), sequentialChains.size(), calculatedFields.size());
        
        return plan;
    }
    
    /**
     * Validate that the dependency graph has no circular dependencies
     */
    public void validateNoCycles(DependencyGraph graph) {
        logger.debug("Validating dependency graph for circular dependencies");
        
        List<String> cycle = graph.detectCircularDependencies();
        if (!cycle.isEmpty()) {
            String cycleDescription = String.join(" -> ", cycle) + " -> " + cycle.get(0);
            throw new IllegalArgumentException(
                "Circular dependency detected in calculated fields: " + cycleDescription);
        }
        
        logger.debug("No circular dependencies found");
    }
    
    /**
     * Group independent data services that can be executed in parallel
     */
    private List<ParallelExecutionGroup> groupIndependentServices(
            Map<String, FieldConfigEntity> dataServiceFields, DependencyGraph graph) {
        
        List<ParallelExecutionGroup> groups = new ArrayList<>();
        Set<String> processedFields = new HashSet<>();
        
        // Get topological order to respect dependencies
        List<String> topologicalOrder = graph.getTopologicalOrder();
        
        // Group fields by dependency level
        Map<Integer, List<FieldConfigEntity>> levelGroups = new HashMap<>();
        
        for (String fieldName : topologicalOrder) {
            if (dataServiceFields.containsKey(fieldName)) {
                FieldConfigEntity field = dataServiceFields.get(fieldName);
                int level = calculateDependencyLevel(fieldName, graph);
                levelGroups.computeIfAbsent(level, k -> new ArrayList<>()).add(field);
            }
        }
        
        // Create parallel groups for each level
        int groupIndex = 0;
        for (Map.Entry<Integer, List<FieldConfigEntity>> entry : levelGroups.entrySet()) {
            int level = entry.getKey();
            List<FieldConfigEntity> fieldsAtLevel = entry.getValue();
            
            // Further group by data service configuration to optimize service calls
            Map<String, List<FieldConfigEntity>> serviceGroups = fieldsAtLevel.stream()
                .collect(Collectors.groupingBy(f -> f.getDataServiceConfigJson()));
            
            for (Map.Entry<String, List<FieldConfigEntity>> serviceGroup : serviceGroups.entrySet()) {
                ParallelExecutionGroup group = new ParallelExecutionGroup(
                    "ParallelGroup_" + groupIndex++);
                group.setFields(serviceGroup.getValue());
                group.setPriority(level);
                groups.add(group);
            }
        }
        
        return groups;
    }
    
    /**
     * Order dependent data services into sequential execution chains
     */
    private List<SequentialExecutionChain> orderDependentServices(
            Map<String, FieldConfigEntity> dataServiceFields, DependencyGraph graph) {
        
        List<SequentialExecutionChain> chains = new ArrayList<>();
        Set<String> processedFields = new HashSet<>();
        
        // Find dependency chains
        for (String fieldName : dataServiceFields.keySet()) {
            if (!processedFields.contains(fieldName)) {
                List<String> chain = findDependencyChain(fieldName, graph, dataServiceFields.keySet());
                if (chain.size() > 1) {
                    SequentialExecutionChain sequentialChain = new SequentialExecutionChain(
                        "SequentialChain_" + chains.size());
                    
                    List<FieldConfigEntity> orderedFields = chain.stream()
                        .map(dataServiceFields::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                    
                    sequentialChain.setOrderedFields(orderedFields);
                    chains.add(sequentialChain);
                    processedFields.addAll(chain);
                }
            }
        }
        
        return chains;
    }
    
    /**
     * Calculate the dependency level of a field (0 = no dependencies, 1 = depends on level 0, etc.)
     */
    private int calculateDependencyLevel(String fieldName, DependencyGraph graph) {
        Set<String> dependencies = graph.getDependencies(fieldName);
        if (dependencies.isEmpty()) {
            return 0;
        }
        
        int maxLevel = 0;
        for (String dependency : dependencies) {
            int depLevel = calculateDependencyLevel(dependency, graph);
            maxLevel = Math.max(maxLevel, depLevel + 1);
        }
        
        return maxLevel;
    }
    
    /**
     * Find a dependency chain starting from a field
     */
    private List<String> findDependencyChain(String startField, DependencyGraph graph, Set<String> dataServiceFields) {
        List<String> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        buildChain(startField, graph, dataServiceFields, chain, visited);
        return chain;
    }
    
    private void buildChain(String field, DependencyGraph graph, Set<String> dataServiceFields, 
                           List<String> chain, Set<String> visited) {
        if (visited.contains(field) || !dataServiceFields.contains(field)) {
            return;
        }
        
        visited.add(field);
        
        // Add dependencies first (they need to be resolved before this field)
        Set<String> dependencies = graph.getDependencies(field);
        for (String dependency : dependencies) {
            if (dataServiceFields.contains(dependency)) {
                buildChain(dependency, graph, dataServiceFields, chain, visited);
            }
        }
        
        // Add this field to the chain
        if (!chain.contains(field)) {
            chain.add(field);
        }
    }
    
    /**
     * Calculate estimated execution time for the resolution plan
     */
    private long calculateEstimatedExecutionTime(List<ParallelExecutionGroup> parallelGroups, 
                                               List<SequentialExecutionChain> sequentialChains) {
        long totalTime = 0;
        
        // For parallel groups, take the maximum time (they run concurrently)
        long maxParallelTime = parallelGroups.stream()
            .mapToLong(group -> estimateGroupExecutionTime(group))
            .max()
            .orElse(0);
        
        // For sequential chains, sum the times (they run sequentially)
        long totalSequentialTime = sequentialChains.stream()
            .mapToLong(chain -> estimateChainExecutionTime(chain))
            .sum();
        
        return maxParallelTime + totalSequentialTime;
    }
    
    private long estimateGroupExecutionTime(ParallelExecutionGroup group) {
        // Estimate based on number of fields and typical service call time
        // This is a simple heuristic - could be made more sophisticated
        return group.getFieldCount() * 100L; // 100ms per field
    }
    
    private long estimateChainExecutionTime(SequentialExecutionChain chain) {
        // Estimate based on number of fields and typical service call time
        return chain.getFieldCount() * 150L; // 150ms per field (sequential overhead)
    }
    
    /**
     * Parse dependencies JSON string into a list of field names
     */
    private List<String> parseDependencies(String dependenciesJson) {
        if (dependenciesJson == null || dependenciesJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(dependenciesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            logger.error("Error parsing dependencies JSON: {}", dependenciesJson, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Analyze data service dependencies and create execution groups
     */
    public List<ParallelExecutionGroup> analyzeDataServiceDependencies(List<FieldConfigEntity> fields) {
        logger.debug("Analyzing data service dependencies for {} fields", fields.size());
        
        DependencyGraph graph = buildDependencyGraph(fields);
        
        // Filter to only data service fields
        Map<String, FieldConfigEntity> dataServiceFields = fields.stream()
            .filter(FieldConfigEntity::hasDataService)
            .collect(Collectors.toMap(FieldConfigEntity::getFieldName, f -> f));
        
        return groupIndependentServices(dataServiceFields, graph);
    }
    
    /**
     * Extract field names from a rule definition for dependency analysis
     */
    public Set<String> extractFieldNamesFromRule(Object ruleDefinition) {
        Set<String> fieldNames = new HashSet<>();
        
        if (ruleDefinition == null) {
            return fieldNames;
        }
        
        try {
            // Convert rule definition to JSON and extract field references
            String ruleJson = objectMapper.writeValueAsString(ruleDefinition);
            extractFieldNamesFromJson(ruleJson, fieldNames);
        } catch (Exception e) {
            logger.error("Error extracting field names from rule definition", e);
        }
        
        return fieldNames;
    }
    
    private void extractFieldNamesFromJson(String json, Set<String> fieldNames) {
        // Simple regex-based extraction - could be made more sophisticated
        // Look for "field" properties in the JSON
        String fieldPattern = "\"field\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(fieldPattern);
        java.util.regex.Matcher matcher = pattern.matcher(json);
        
        while (matcher.find()) {
            fieldNames.add(matcher.group(1));
        }
    }
    
    /**
     * Enhance the resolution plan with calculated field information
     */
    private void enhanceWithCalculatedFields(FieldResolutionPlan plan, 
                                           Map<String, FieldConfigEntity> calculatedFields,
                                           DependencyGraph graph) {
        logger.debug("Enhancing plan with {} calculated fields", calculatedFields.size());
        
        // Calculate dependency levels for calculated fields
        Map<String, Integer> calculatedFieldLevels = new HashMap<>();
        List<String> calculatedFieldOrder = new ArrayList<>();
        
        // Get topological order for calculated fields
        List<String> topologicalOrder = graph.getTopologicalOrder();
        
        for (String fieldName : topologicalOrder) {
            if (calculatedFields.containsKey(fieldName)) {
                int level = calculateDependencyLevel(fieldName, graph);
                calculatedFieldLevels.put(fieldName, level);
                calculatedFieldOrder.add(fieldName);
            }
        }
        
        plan.setCalculatedFieldLevels(calculatedFieldLevels);
        plan.setCalculatedFieldOrder(calculatedFieldOrder);
        
        logger.debug("Enhanced plan with calculated field levels: {}", calculatedFieldLevels);
    }
    
    /**
     * Create optimized execution plan for data service dependencies
     */
    public FieldResolutionPlan createOptimizedExecutionPlan(List<FieldConfigEntity> fields) {
        logger.debug("Creating optimized execution plan for {} fields", fields.size());
        
        DependencyGraph graph = buildDependencyGraph(fields);
        FieldResolutionPlan plan = createResolutionPlan(graph);
        
        // Additional optimizations
        optimizeParallelExecution(plan);
        optimizeSequentialExecution(plan);
        
        return plan;
    }
    
    /**
     * Optimize parallel execution groups
     */
    private void optimizeParallelExecution(FieldResolutionPlan plan) {
        if (!plan.hasParallelExecution()) {
            return;
        }
        
        // Sort groups by estimated execution time (shortest first for better parallelization)
        plan.getParallelGroups().sort((g1, g2) -> 
            Long.compare(g1.getEstimatedExecutionTimeMs(), g2.getEstimatedExecutionTimeMs()));
        
        // Merge small groups if beneficial
        mergeSmallParallelGroups(plan.getParallelGroups());
        
        logger.debug("Optimized {} parallel groups", plan.getParallelGroups().size());
    }
    
    /**
     * Optimize sequential execution chains
     */
    private void optimizeSequentialExecution(FieldResolutionPlan plan) {
        if (!plan.hasSequentialExecution()) {
            return;
        }
        
        // Sort chains by priority and estimated execution time
        plan.getSequentialChains().sort((c1, c2) -> {
            int priorityCompare = Integer.compare(c1.getPriority(), c2.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Long.compare(c1.getEstimatedExecutionTimeMs(), c2.getEstimatedExecutionTimeMs());
        });
        
        logger.debug("Optimized {} sequential chains", plan.getSequentialChains().size());
    }
    
    /**
     * Merge small parallel groups for better efficiency
     */
    private void mergeSmallParallelGroups(List<ParallelExecutionGroup> groups) {
        // Simple heuristic: merge groups with less than 3 fields
        List<ParallelExecutionGroup> smallGroups = groups.stream()
            .filter(group -> group.getFieldCount() < 3)
            .collect(Collectors.toList());
        
        if (smallGroups.size() > 1) {
            ParallelExecutionGroup mergedGroup = new ParallelExecutionGroup("MergedGroup_Small");
            
            for (ParallelExecutionGroup smallGroup : smallGroups) {
                mergedGroup.getFields().addAll(smallGroup.getFields());
                groups.remove(smallGroup);
            }
            
            groups.add(mergedGroup);
            logger.debug("Merged {} small groups into one group", smallGroups.size());
        }
    }
}