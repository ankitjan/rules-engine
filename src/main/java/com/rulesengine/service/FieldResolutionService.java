package com.rulesengine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.analyzer.DependencyAnalyzer;
import com.rulesengine.client.config.DataServiceConfig;
import com.rulesengine.client.config.GraphQLServiceConfig;
import com.rulesengine.client.config.RestServiceConfig;
import com.rulesengine.dto.*;
import com.rulesengine.entity.FieldConfigEntity;
import com.rulesengine.exception.DataServiceException;
import com.rulesengine.exception.FieldMappingException;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.model.RuleItem;
import com.rulesengine.repository.FieldConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for dynamic field value resolution from external data services with dependency analysis
 */
@Service
@Transactional
public class FieldResolutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FieldResolutionService.class);
    
    private final FieldConfigRepository fieldConfigRepository;
    private final DataServiceClient dataServiceClient;
    private final FieldMappingService fieldMappingService;
    private final FieldCalculatorService fieldCalculatorService;
    private final DependencyAnalyzer dependencyAnalyzer;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    
    // Cache for field values during execution - organized by execution context
    private final Map<String, Map<String, Object>> executionCache = new ConcurrentHashMap<>();
    
    // Cache for calculated field values within execution context
    private final Map<String, Map<String, Object>> calculatedFieldCache = new ConcurrentHashMap<>();
    
    @Autowired
    public FieldResolutionService(FieldConfigRepository fieldConfigRepository,
                                 DataServiceClient dataServiceClient,
                                 FieldMappingService fieldMappingService,
                                 FieldCalculatorService fieldCalculatorService,
                                 DependencyAnalyzer dependencyAnalyzer,
                                 ObjectMapper objectMapper) {
        this.fieldConfigRepository = fieldConfigRepository;
        this.dataServiceClient = dataServiceClient;
        this.fieldMappingService = fieldMappingService;
        this.fieldCalculatorService = fieldCalculatorService;
        this.dependencyAnalyzer = dependencyAnalyzer;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(10); // Configurable thread pool
    }
    
    /**
     * Resolve field values for the given field names using the execution context
     * Enhanced with calculated field integration and improved caching
     */
    @Transactional(readOnly = true)
    public Map<String, Object> resolveFields(List<String> fieldNames, ExecutionContext context) {
        logger.info("Resolving {} fields for entity: {}", fieldNames.size(), context.getEntityId());
        
        long startTime = System.currentTimeMillis();
        String cacheKey = generateCacheKey(context);
        
        try {
            // Check if we have cached results for this execution context
            Map<String, Object> cachedResults = getCachedFieldValues(cacheKey, fieldNames);
            if (cachedResults != null && cachedResults.size() == fieldNames.size()) {
                logger.debug("Returning fully cached results for {} fields", fieldNames.size());
                return cachedResults;
            }
            
            // Create resolution plan with enhanced dependency analysis
            FieldResolutionPlan plan = createResolutionPlan(fieldNames);
            
            // Execute resolution plan with calculated field integration
            FieldResolutionResult result = executeResolutionPlan(plan, context, cacheKey);
            
            // Cache the resolved values for future use
            cacheResolvedValues(cacheKey, result.getResolvedValues());
            
            long endTime = System.currentTimeMillis();
            logger.info("Field resolution completed in {}ms. Success rate: {:.2f}%", 
                endTime - startTime, result.getSuccessRate() * 100);
            
            return result.getResolvedValues();
            
        } catch (Exception e) {
            logger.error("Error resolving fields: {}", e.getMessage(), e);
            
            // Return field values from context as fallback
            Map<String, Object> fallbackValues = new HashMap<>(context.getFieldValues());
            
            return fallbackValues;
        }
    }
    
    /**
     * Create a field resolution plan based on field dependencies and data service configurations
     * Enhanced with better calculated field integration
     */
    @Transactional(readOnly = true)
    public FieldResolutionPlan createResolutionPlan(List<String> fieldNames) {
        logger.debug("Creating resolution plan for {} fields", fieldNames.size());
        
        // Get field configurations
        List<FieldConfigEntity> fieldConfigs = fieldConfigRepository.findByFieldNamesActive(fieldNames);
        
        // Separate fields by type for better planning
        Map<String, FieldConfigEntity> allFields = fieldConfigs.stream()
            .collect(Collectors.toMap(FieldConfigEntity::getFieldName, f -> f));
        
        List<FieldConfigEntity> dataServiceFields = fieldConfigs.stream()
            .filter(f -> !f.getIsCalculated() && f.hasDataService())
            .collect(Collectors.toList());
        
        List<FieldConfigEntity> calculatedFields = fieldConfigs.stream()
            .filter(FieldConfigEntity::getIsCalculated)
            .collect(Collectors.toList());
        
        List<FieldConfigEntity> staticFields = fieldConfigs.stream()
            .filter(f -> !f.getIsCalculated() && !f.hasDataService())
            .collect(Collectors.toList());
        
        // Use DependencyAnalyzer to create the resolution plan
        DependencyGraph graph = dependencyAnalyzer.buildDependencyGraph(fieldConfigs);
        FieldResolutionPlan plan = dependencyAnalyzer.createResolutionPlan(graph);
        
        // Add static field values for fields without data services or calculations
        Map<String, Object> staticValues = new HashMap<>();
        for (FieldConfigEntity field : staticFields) {
            if (field.hasDefaultValue()) {
                staticValues.put(field.getFieldName(), parseDefaultValue(field.getDefaultValue(), field.getFieldType()));
            }
        }
        plan.setStaticFieldValues(staticValues);
        
        // Enhance the plan with calculated field information
        enhancePlanWithCalculatedFields(plan, calculatedFields, allFields);
        
        logger.debug("Created enhanced resolution plan: {}", plan);
        
        return plan;
    }
    
    /**
     * Execute the field resolution plan with enhanced calculated field integration
     */
    public FieldResolutionResult executeResolutionPlan(FieldResolutionPlan plan, ExecutionContext context, String cacheKey) {
        logger.debug("Executing enhanced resolution plan: {}", plan);
        
        long startTime = System.currentTimeMillis();
        FieldResolutionResult result = new FieldResolutionResult();
        
        try {
            // Add static field values first
            if (plan.getStaticFieldValues() != null) {
                for (Map.Entry<String, Object> entry : plan.getStaticFieldValues().entrySet()) {
                    result.addResolvedValue(entry.getKey(), entry.getValue());
                }
            }
            
            // Add context field values
            if (context.getFieldValues() != null) {
                for (Map.Entry<String, Object> entry : context.getFieldValues().entrySet()) {
                    if (!result.hasResolvedValue(entry.getKey())) {
                        result.addResolvedValue(entry.getKey(), entry.getValue());
                    }
                }
            }
            
            // Execute parallel groups with enhanced error handling
            if (plan.hasParallelExecution()) {
                executeParallelGroupsEnhanced(plan.getParallelGroups(), context, cacheKey, result);
            }
            
            // Execute sequential chains with dependency tracking
            if (plan.hasSequentialExecution()) {
                executeSequentialChainsEnhanced(plan.getSequentialChains(), context, cacheKey, result);
            }
            
            // Calculate calculated fields with enhanced caching
            calculateFieldsInPlanEnhanced(plan, result, cacheKey);
            
            long endTime = System.currentTimeMillis();
            result.setTotalExecutionTimeMs(endTime - startTime);
            
            logger.debug("Enhanced resolution plan executed successfully: {}", result);
            
        } catch (Exception e) {
            logger.error("Error executing resolution plan: {}", e.getMessage(), e);
            result.markAsCompleteFailure(e.getMessage());
            result.setTotalExecutionTimeMs(System.currentTimeMillis() - startTime);
        }
        
        return result;
    }
    
    /**
     * Execute parallel execution groups concurrently
     */
    private void executeParallelGroups(List<ParallelExecutionGroup> groups, ExecutionContext context, 
                                     String cacheKey, FieldResolutionResult result) {
        logger.debug("Executing {} parallel groups", groups.size());
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (ParallelExecutionGroup group : groups) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                executeParallelGroup(group, context, cacheKey, result);
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all parallel executions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        logger.debug("All parallel groups completed");
    }
    
    /**
     * Execute a single parallel group
     */
    private void executeParallelGroup(ParallelExecutionGroup group, ExecutionContext context, 
                                    String cacheKey, FieldResolutionResult result) {
        logger.debug("Executing parallel group: {}", group.getGroupName());
        
        for (FieldConfigEntity field : group.getFields()) {
            try {
                Object value = resolveFieldFromDataService(field, context, cacheKey);
                synchronized (result) {
                    result.addResolvedValue(field.getFieldName(), value);
                }
            } catch (Exception e) {
                logger.warn("Failed to resolve field '{}' in parallel group: {}", 
                    field.getFieldName(), e.getMessage());
                synchronized (result) {
                    result.addResolutionError(field.getFieldName(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Execute sequential execution chains
     */
    private void executeSequentialChains(List<SequentialExecutionChain> chains, ExecutionContext context, 
                                       String cacheKey, FieldResolutionResult result) {
        logger.debug("Executing {} sequential chains", chains.size());
        
        for (SequentialExecutionChain chain : chains) {
            executeSequentialChain(chain, context, cacheKey, result);
        }
        
        logger.debug("All sequential chains completed");
    }
    
    /**
     * Execute a single sequential chain
     */
    private void executeSequentialChain(SequentialExecutionChain chain, ExecutionContext context, 
                                      String cacheKey, FieldResolutionResult result) {
        logger.debug("Executing sequential chain: {}", chain.getChainName());
        
        for (FieldConfigEntity field : chain.getOrderedFields()) {
            try {
                Object value;
                if (field.getIsCalculated()) {
                    // Calculated fields will be handled separately after all data service fields are resolved
                    logger.debug("Skipping calculated field '{}' in sequential chain - will be calculated later", 
                               field.getFieldName());
                    continue;
                } else {
                    value = resolveFieldFromDataService(field, context, cacheKey);
                }
                
                result.addResolvedValue(field.getFieldName(), value);
                
                // Update context for dependent fields
                context.addFieldValue(field.getFieldName(), value);
                
            } catch (Exception e) {
                logger.warn("Failed to resolve field '{}' in sequential chain: {}", 
                    field.getFieldName(), e.getMessage());
                result.addResolutionError(field.getFieldName(), e.getMessage());
                
                // For sequential chains, we might want to continue or stop based on configuration
                // For now, we continue with the next field
            }
        }
    }
    
    /**
     * Resolve a single field from its configured data service
     */
    @Cacheable(value = "fieldValues", key = "#cacheKey + '_' + #field.fieldName")
    private Object resolveFieldFromDataService(FieldConfigEntity field, ExecutionContext context, String cacheKey) {
        logger.debug("Resolving field '{}' from data service", field.getFieldName());
        
        if (!field.hasDataService()) {
            return field.hasDefaultValue() ? field.getDefaultValue() : null;
        }
        
        try {
            // Parse data service configuration
            DataServiceConfig serviceConfig = parseDataServiceConfig(field.getDataServiceConfigJson());
            
            // Prepare parameters for the service call
            Map<String, Object> parameters = prepareServiceParameters(context, field);
            
            // Execute data service request
            Object response = dataServiceClient.executeRequest(serviceConfig, parameters);
            
            // Extract field value using mapper
            Object value = fieldMappingService.extractFieldValue(response, field);
            
            logger.debug("Successfully resolved field '{}' with value type: {}", 
                field.getFieldName(), value != null ? value.getClass().getSimpleName() : "null");
            
            return value;
            
        } catch (Exception e) {
            logger.error("Error resolving field '{}' from data service: {}", field.getFieldName(), e.getMessage(), e);
            
            // Use default value if available
            if (field.hasDefaultValue()) {
                logger.debug("Using default value for field '{}'", field.getFieldName());
                return field.getDefaultValue();
            }
            
            // Re-throw for required fields
            if (field.getIsRequired()) {
                throw new FieldMappingException("Required field resolution failed: " + e.getMessage(), 
                    field.getMapperExpression(), field.getFieldName(), e);
            }
            
            return null;
        }
    }
    
    /**
     * Parse data service configuration from JSON
     */
    private DataServiceConfig parseDataServiceConfig(String configJson) {
        try {
            Map<String, Object> configMap = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
            String serviceType = (String) configMap.get("serviceType");
            
            if ("GRAPHQL".equals(serviceType)) {
                return objectMapper.readValue(configJson, GraphQLServiceConfig.class);
            } else if ("REST".equals(serviceType)) {
                return objectMapper.readValue(configJson, RestServiceConfig.class);
            } else {
                throw new DataServiceException("Unsupported service type: " + serviceType);
            }
            
        } catch (Exception e) {
            throw new DataServiceException("Failed to parse data service configuration: " + e.getMessage(), e);
        }
    }
    
    /**
     * Prepare parameters for data service calls
     */
    private Map<String, Object> prepareServiceParameters(ExecutionContext context, FieldConfigEntity field) {
        Map<String, Object> parameters = new HashMap<>();
        
        // Add entity information
        if (context.getEntityId() != null) {
            parameters.put("entityId", context.getEntityId());
        }
        if (context.getEntityType() != null) {
            parameters.put("entityType", context.getEntityType());
        }
        
        // Add field values that might be needed as parameters
        if (context.getFieldValues() != null) {
            parameters.putAll(context.getFieldValues());
        }
        
        // Add metadata
        if (context.getMetadata() != null) {
            parameters.putAll(context.getMetadata());
        }
        
        return parameters;
    }
    
    /**
     * Generate cache key for execution context
     */
    private String generateCacheKey(ExecutionContext context) {
        StringBuilder keyBuilder = new StringBuilder();
        
        if (context.getEntityId() != null) {
            keyBuilder.append("entity:").append(context.getEntityId());
        }
        if (context.getEntityType() != null) {
            keyBuilder.append("_type:").append(context.getEntityType());
        }
        
        // Add a timestamp component to ensure cache freshness (5-minute buckets)
        long timeBucket = System.currentTimeMillis() / (5 * 60 * 1000);
        keyBuilder.append("_time:").append(timeBucket);
        
        return keyBuilder.toString();
    }
    
    /**
     * Extract field names from a rule definition for dependency analysis
     */
    public List<String> extractFieldNamesFromRule(RuleDefinition ruleDefinition) {
        List<String> fieldNames = new ArrayList<>();
        
        if (ruleDefinition != null && ruleDefinition.getRules() != null) {
            extractFieldNamesFromItems(ruleDefinition.getRules(), fieldNames);
        }
        
        return fieldNames;
    }
    
    /**
     * Recursively extract field names from rule items
     */
    private void extractFieldNamesFromItems(List<RuleItem> items, List<String> fieldNames) {
        if (items == null) {
            return;
        }
        
        for (RuleItem item : items) {
            if (item.isCondition() && item.getField() != null) {
                if (!fieldNames.contains(item.getField())) {
                    fieldNames.add(item.getField());
                }
            } else if (item.isGroup() && item.getRules() != null) {
                extractFieldNamesFromItems(item.getRules(), fieldNames);
            }
        }
    }
    
    /**
     * Clear execution cache for a specific context
     */
    public void clearExecutionCache(String cacheKey) {
        executionCache.remove(cacheKey);
        logger.debug("Cleared execution cache for key: {}", cacheKey);
    }
    
    /**
     * Clear all execution cache
     */
    public void clearAllExecutionCache() {
        executionCache.clear();
        logger.debug("Cleared all execution cache");
    }
    
    /**
     * Calculate calculated fields in the resolution plan
     */
    private void calculateFieldsInPlan(FieldResolutionPlan plan, FieldResolutionResult result) {
        // Collect all calculated fields from the plan
        List<FieldConfigEntity> calculatedFields = new ArrayList<>();
        
        // Get calculated fields from parallel groups
        if (plan.getParallelGroups() != null) {
            for (ParallelExecutionGroup group : plan.getParallelGroups()) {
                calculatedFields.addAll(group.getFields().stream()
                    .filter(FieldConfigEntity::getIsCalculated)
                    .collect(Collectors.toList()));
            }
        }
        
        // Get calculated fields from sequential chains
        if (plan.getSequentialChains() != null) {
            for (SequentialExecutionChain chain : plan.getSequentialChains()) {
                calculatedFields.addAll(chain.getOrderedFields().stream()
                    .filter(FieldConfigEntity::getIsCalculated)
                    .collect(Collectors.toList()));
            }
        }
        
        if (!calculatedFields.isEmpty()) {
            try {
                Map<String, Object> calculatedValues = fieldCalculatorService.calculateFields(
                    calculatedFields, result.getResolvedValues());
                
                // Add calculated values to result
                for (Map.Entry<String, Object> entry : calculatedValues.entrySet()) {
                    result.addResolvedValue(entry.getKey(), entry.getValue());
                }
                
                logger.debug("Calculated {} fields successfully", calculatedFields.size());
            } catch (Exception e) {
                logger.error("Error calculating fields: {}", e.getMessage(), e);
                for (FieldConfigEntity field : calculatedFields) {
                    result.addResolutionError(field.getFieldName(), "Calculation failed: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Parse default value based on field type
     */
    private Object parseDefaultValue(String defaultValue, String fieldType) {
        if (defaultValue == null) {
            return null;
        }
        
        try {
            switch (fieldType.toUpperCase()) {
                case "NUMBER":
                    return Double.parseDouble(defaultValue);
                case "BOOLEAN":
                    return Boolean.parseBoolean(defaultValue);
                case "STRING":
                default:
                    return defaultValue;
            }
        } catch (Exception e) {
            logger.warn("Error parsing default value '{}' for type '{}', using as string", 
                       defaultValue, fieldType);
            return defaultValue;
        }
    }
    
    /**
     * Enhance the resolution plan with calculated field information
     */
    private void enhancePlanWithCalculatedFields(FieldResolutionPlan plan, 
                                               List<FieldConfigEntity> calculatedFields,
                                               Map<String, FieldConfigEntity> allFields) {
        if (calculatedFields.isEmpty()) {
            return;
        }
        
        // Create a separate dependency graph for calculated fields
        DependencyGraph calculatedGraph = dependencyAnalyzer.buildDependencyGraph(calculatedFields);
        
        // Add calculated fields to existing execution groups based on their dependencies
        for (FieldConfigEntity calculatedField : calculatedFields) {
            addCalculatedFieldToExecutionGroups(plan, calculatedField, allFields);
        }
        
        logger.debug("Enhanced plan with {} calculated fields", calculatedFields.size());
    }
    
    /**
     * Add calculated field to appropriate execution groups based on dependencies
     */
    private void addCalculatedFieldToExecutionGroups(FieldResolutionPlan plan, 
                                                   FieldConfigEntity calculatedField,
                                                   Map<String, FieldConfigEntity> allFields) {
        try {
            List<String> dependencies = parseDependencies(calculatedField.getDependenciesJson());
            
            // Determine if this calculated field can be added to parallel groups or needs sequential execution
            boolean hasDataServiceDependencies = dependencies.stream()
                .anyMatch(dep -> {
                    FieldConfigEntity depField = allFields.get(dep);
                    return depField != null && depField.hasDataService();
                });
            
            if (hasDataServiceDependencies) {
                // Add to sequential chains since it depends on data service fields
                addCalculatedFieldToSequentialChains(plan, calculatedField, dependencies);
            } else {
                // Can be calculated after parallel execution
                addCalculatedFieldToParallelGroups(plan, calculatedField);
            }
            
        } catch (Exception e) {
            logger.warn("Error processing calculated field '{}': {}", 
                       calculatedField.getFieldName(), e.getMessage());
        }
    }
    
    /**
     * Add calculated field to sequential chains
     */
    private void addCalculatedFieldToSequentialChains(FieldResolutionPlan plan, 
                                                    FieldConfigEntity calculatedField,
                                                    List<String> dependencies) {
        // Find the appropriate sequential chain or create a new one
        SequentialExecutionChain targetChain = null;
        
        for (SequentialExecutionChain chain : plan.getSequentialChains()) {
            // Check if any of the dependencies are in this chain
            boolean hasDependencyInChain = dependencies.stream()
                .anyMatch(dep -> chain.containsField(dep));
            
            if (hasDependencyInChain) {
                targetChain = chain;
                break;
            }
        }
        
        if (targetChain == null) {
            // Create a new chain for this calculated field
            targetChain = new SequentialExecutionChain("CalculatedChain_" + plan.getSequentialChains().size());
            plan.getSequentialChains().add(targetChain);
        }
        
        targetChain.addField(calculatedField);
    }
    
    /**
     * Add calculated field to parallel groups
     */
    private void addCalculatedFieldToParallelGroups(FieldResolutionPlan plan, FieldConfigEntity calculatedField) {
        // Create a special parallel group for calculated fields that don't depend on data services
        ParallelExecutionGroup calculatedGroup = plan.getParallelGroups().stream()
            .filter(group -> group.getGroupName().startsWith("CalculatedGroup"))
            .findFirst()
            .orElse(null);
        
        if (calculatedGroup == null) {
            calculatedGroup = new ParallelExecutionGroup("CalculatedGroup_Independent");
            calculatedGroup.setPriority(1000); // High priority to execute after data service groups
            plan.getParallelGroups().add(calculatedGroup);
        }
        
        calculatedGroup.addField(calculatedField);
    }
    
    /**
     * Execute parallel groups with enhanced error handling and caching
     */
    private void executeParallelGroupsEnhanced(List<ParallelExecutionGroup> groups, ExecutionContext context, 
                                             String cacheKey, FieldResolutionResult result) {
        logger.debug("Executing {} parallel groups with enhanced features", groups.size());
        
        // Sort groups by priority
        List<ParallelExecutionGroup> sortedGroups = groups.stream()
            .sorted(Comparator.comparingInt(ParallelExecutionGroup::getPriority))
            .collect(Collectors.toList());
        
        for (ParallelExecutionGroup group : sortedGroups) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (FieldConfigEntity field : group.getFields()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    executeFieldWithCaching(field, context, cacheKey, result);
                }, executorService);
                
                futures.add(future);
            }
            
            // Wait for all fields in this group to complete before moving to next group
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            logger.debug("Completed parallel group: {}", group.getGroupName());
        }
        
        logger.debug("All enhanced parallel groups completed");
    }
    
    /**
     * Execute sequential chains with enhanced dependency tracking
     */
    private void executeSequentialChainsEnhanced(List<SequentialExecutionChain> chains, ExecutionContext context, 
                                                String cacheKey, FieldResolutionResult result) {
        logger.debug("Executing {} sequential chains with enhanced features", chains.size());
        
        // Sort chains by priority
        List<SequentialExecutionChain> sortedChains = chains.stream()
            .sorted(Comparator.comparingInt(SequentialExecutionChain::getPriority))
            .collect(Collectors.toList());
        
        for (SequentialExecutionChain chain : sortedChains) {
            executeSequentialChainEnhanced(chain, context, cacheKey, result);
        }
        
        logger.debug("All enhanced sequential chains completed");
    }
    
    /**
     * Execute a single sequential chain with enhanced features
     */
    private void executeSequentialChainEnhanced(SequentialExecutionChain chain, ExecutionContext context, 
                                               String cacheKey, FieldResolutionResult result) {
        logger.debug("Executing enhanced sequential chain: {}", chain.getChainName());
        
        for (FieldConfigEntity field : chain.getOrderedFields()) {
            try {
                if (field.getIsCalculated()) {
                    // Handle calculated fields with enhanced caching
                    Object value = calculateFieldWithCaching(field, result.getResolvedValues(), cacheKey);
                    result.addResolvedValue(field.getFieldName(), value);
                } else {
                    // Handle data service fields
                    executeFieldWithCaching(field, context, cacheKey, result);
                }
                
                // Update context for dependent fields
                Object resolvedValue = result.getResolvedValue(field.getFieldName());
                if (resolvedValue != null) {
                    context.addFieldValue(field.getFieldName(), resolvedValue);
                }
                
            } catch (Exception e) {
                logger.warn("Failed to resolve field '{}' in enhanced sequential chain: {}", 
                    field.getFieldName(), e.getMessage());
                result.addResolutionError(field.getFieldName(), e.getMessage());
            }
        }
    }
    
    /**
     * Execute a single field with caching support
     */
    private void executeFieldWithCaching(FieldConfigEntity field, ExecutionContext context, 
                                       String cacheKey, FieldResolutionResult result) {
        try {
            // Check cache first
            String fieldCacheKey = cacheKey + "_" + field.getFieldName();
            Object cachedValue = getCachedFieldValue(fieldCacheKey);
            
            if (cachedValue != null) {
                logger.debug("Using cached value for field '{}'", field.getFieldName());
                synchronized (result) {
                    result.addResolvedValue(field.getFieldName(), cachedValue);
                }
                return;
            }
            
            // Resolve from data service
            Object value = resolveFieldFromDataService(field, context, cacheKey);
            
            // Cache the resolved value
            cacheFieldValue(fieldCacheKey, value);
            
            synchronized (result) {
                result.addResolvedValue(field.getFieldName(), value);
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve field '{}' with caching: {}", 
                field.getFieldName(), e.getMessage());
            synchronized (result) {
                result.addResolutionError(field.getFieldName(), e.getMessage());
            }
        }
    }
    
    /**
     * Calculate calculated fields with enhanced caching
     */
    private void calculateFieldsInPlanEnhanced(FieldResolutionPlan plan, FieldResolutionResult result, String cacheKey) {
        // Collect all calculated fields from the plan
        List<FieldConfigEntity> calculatedFields = new ArrayList<>();
        
        // Get calculated fields from parallel groups
        if (plan.getParallelGroups() != null) {
            for (ParallelExecutionGroup group : plan.getParallelGroups()) {
                calculatedFields.addAll(group.getFields().stream()
                    .filter(FieldConfigEntity::getIsCalculated)
                    .collect(Collectors.toList()));
            }
        }
        
        // Get calculated fields from sequential chains
        if (plan.getSequentialChains() != null) {
            for (SequentialExecutionChain chain : plan.getSequentialChains()) {
                calculatedFields.addAll(chain.getOrderedFields().stream()
                    .filter(FieldConfigEntity::getIsCalculated)
                    .collect(Collectors.toList()));
            }
        }
        
        if (!calculatedFields.isEmpty()) {
            try {
                // Use enhanced calculated field service with caching
                Map<String, Object> calculatedValues = calculateFieldsWithCaching(
                    calculatedFields, result.getResolvedValues(), cacheKey);
                
                // Add calculated values to result
                for (Map.Entry<String, Object> entry : calculatedValues.entrySet()) {
                    result.addResolvedValue(entry.getKey(), entry.getValue());
                }
                
                logger.debug("Calculated {} fields successfully with enhanced caching", calculatedFields.size());
            } catch (Exception e) {
                logger.error("Error calculating fields with enhanced features: {}", e.getMessage(), e);
                for (FieldConfigEntity field : calculatedFields) {
                    result.addResolutionError(field.getFieldName(), "Calculation failed: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Calculate a single field with caching
     */
    private Object calculateFieldWithCaching(FieldConfigEntity field, Map<String, Object> dependentValues, String cacheKey) {
        String fieldCacheKey = cacheKey + "_calc_" + field.getFieldName();
        
        // Check calculated field cache first
        Object cachedValue = getCachedCalculatedFieldValue(fieldCacheKey);
        if (cachedValue != null) {
            logger.debug("Using cached calculated value for field '{}'", field.getFieldName());
            return cachedValue;
        }
        
        // Calculate the field value
        Object calculatedValue = fieldCalculatorService.calculateField(field, dependentValues);
        
        // Cache the calculated value
        cacheCalculatedFieldValue(fieldCacheKey, calculatedValue);
        
        return calculatedValue;
    }
    
    /**
     * Calculate multiple fields with caching support
     */
    private Map<String, Object> calculateFieldsWithCaching(List<FieldConfigEntity> calculatedFields, 
                                                         Map<String, Object> baseFieldValues, 
                                                         String cacheKey) {
        Map<String, Object> results = new HashMap<>();
        
        // Check cache for each calculated field
        for (FieldConfigEntity field : calculatedFields) {
            String fieldCacheKey = cacheKey + "_calc_" + field.getFieldName();
            Object cachedValue = getCachedCalculatedFieldValue(fieldCacheKey);
            
            if (cachedValue != null) {
                results.put(field.getFieldName(), cachedValue);
                logger.debug("Using cached calculated value for field '{}'", field.getFieldName());
            }
        }
        
        // Calculate remaining fields that weren't cached
        List<FieldConfigEntity> fieldsToCalculate = calculatedFields.stream()
            .filter(field -> !results.containsKey(field.getFieldName()))
            .collect(Collectors.toList());
        
        if (!fieldsToCalculate.isEmpty()) {
            Map<String, Object> newCalculations = fieldCalculatorService.calculateFields(fieldsToCalculate, baseFieldValues);
            
            // Cache the new calculations
            for (Map.Entry<String, Object> entry : newCalculations.entrySet()) {
                String fieldCacheKey = cacheKey + "_calc_" + entry.getKey();
                cacheCalculatedFieldValue(fieldCacheKey, entry.getValue());
                results.put(entry.getKey(), entry.getValue());
            }
        }
        
        return results;
    }
    
    /**
     * Get cached field values for the given field names
     */
    private Map<String, Object> getCachedFieldValues(String cacheKey, List<String> fieldNames) {
        Map<String, Object> cachedValues = executionCache.get(cacheKey);
        if (cachedValues == null) {
            return null;
        }
        
        Map<String, Object> result = new HashMap<>();
        for (String fieldName : fieldNames) {
            if (cachedValues.containsKey(fieldName)) {
                result.put(fieldName, cachedValues.get(fieldName));
            }
        }
        
        return result.size() == fieldNames.size() ? result : null;
    }
    
    /**
     * Get cached field value
     */
    private Object getCachedFieldValue(String fieldCacheKey) {
        return executionCache.values().stream()
            .filter(cache -> cache.containsKey(fieldCacheKey))
            .findFirst()
            .map(cache -> cache.get(fieldCacheKey))
            .orElse(null);
    }
    
    /**
     * Cache field value
     */
    private void cacheFieldValue(String fieldCacheKey, Object value) {
        // Simple implementation - could be enhanced with TTL and size limits
        String cacheKey = fieldCacheKey.substring(0, fieldCacheKey.lastIndexOf("_"));
        executionCache.computeIfAbsent(cacheKey, k -> new ConcurrentHashMap<>()).put(fieldCacheKey, value);
    }
    
    /**
     * Cache resolved field values
     */
    private void cacheResolvedValues(String cacheKey, Map<String, Object> resolvedValues) {
        executionCache.put(cacheKey, new HashMap<>(resolvedValues));
        logger.debug("Cached {} resolved values for key: {}", resolvedValues.size(), cacheKey);
    }
    
    /**
     * Get cached calculated field value
     */
    private Object getCachedCalculatedFieldValue(String fieldCacheKey) {
        return calculatedFieldCache.values().stream()
            .filter(cache -> cache.containsKey(fieldCacheKey))
            .findFirst()
            .map(cache -> cache.get(fieldCacheKey))
            .orElse(null);
    }
    
    /**
     * Cache calculated field value
     */
    private void cacheCalculatedFieldValue(String fieldCacheKey, Object value) {
        String cacheKey = fieldCacheKey.substring(0, fieldCacheKey.lastIndexOf("_calc_"));
        calculatedFieldCache.computeIfAbsent(cacheKey, k -> new ConcurrentHashMap<>()).put(fieldCacheKey, value);
        logger.debug("Cached calculated field value for key: {}", fieldCacheKey);
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
}