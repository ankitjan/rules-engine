package com.rulesengine.service;

import com.rulesengine.dto.EntityFilterRequest;
import com.rulesengine.dto.EntityFilterResult;
import com.rulesengine.dto.EntityFilterResult.FilteredEntity;
import com.rulesengine.dto.EntityFilterResult.FilteringMetrics;
import com.rulesengine.dto.EntityFilterResult.PaginationInfo;
import com.rulesengine.dto.EntityFilterResult.EntityProcessingError;
import com.rulesengine.dto.ExecutionContext;
import com.rulesengine.dto.RuleExecutionResult;
import com.rulesengine.entity.EntityTypeEntity;
import com.rulesengine.exception.DataServiceException;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.repository.EntityTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Transactional
public class EntityFilterService {

    private static final Logger logger = LoggerFactory.getLogger(EntityFilterService.class);

    private final EntityTypeRepository entityTypeRepository;
    private final EntityTypeService entityTypeService;
    private final RuleExecutionService ruleExecutionService;
    private final FieldResolutionService fieldResolutionService;
    private final ExecutorService executorService;

    @Autowired
    public EntityFilterService(EntityTypeRepository entityTypeRepository,
                              EntityTypeService entityTypeService,
                              RuleExecutionService ruleExecutionService,
                              FieldResolutionService fieldResolutionService) {
        this.entityTypeRepository = entityTypeRepository;
        this.entityTypeService = entityTypeService;
        this.ruleExecutionService = ruleExecutionService;
        this.fieldResolutionService = fieldResolutionService;
        this.executorService = Executors.newFixedThreadPool(10); // Configurable thread pool
    }

    /**
     * Filter entities using a rule definition with pagination support
     */
    @Transactional(readOnly = true)
    public EntityFilterResult filterEntities(EntityFilterRequest request) {
        logger.info("Filtering entities for type: {} with pagination", request.getEntityType());

        long startTime = System.currentTimeMillis();
        
        // Validate entity type exists
        EntityTypeEntity entityType = validateEntityType(request.getEntityType());
        
        // Get entity IDs to process
        List<String> entityIds = getEntityIdsToProcess(request, entityType);
        
        // Apply pagination to entity IDs
        List<String> paginatedEntityIds = applyPagination(entityIds, request.getPage(), request.getSize());
        
        // Process entities
        EntityFilterResult result = processEntities(paginatedEntityIds, request, entityType, startTime);
        
        // Set pagination info
        PaginationInfo pagination = new PaginationInfo(
            request.getPage(), 
            request.getSize(), 
            (long) entityIds.size()
        );
        result.setPagination(pagination);
        
        logger.info("Entity filtering completed: processed={}, matched={}, failed={}", 
                   result.getTotalProcessed(), result.getTotalMatched(), result.getTotalFailed());
        
        return result;
    }

    /**
     * Filter entities using batch processing for better performance
     */
    @Transactional(readOnly = true)
    public EntityFilterResult filterEntitiesBatch(EntityFilterRequest request) {
        logger.info("Batch filtering entities for type: {} with batch size: {}", 
                   request.getEntityType(), request.getBatchSize());

        long startTime = System.currentTimeMillis();
        
        // Validate entity type exists
        EntityTypeEntity entityType = validateEntityType(request.getEntityType());
        
        // Get entity IDs to process
        List<String> entityIds = getEntityIdsToProcess(request, entityType);
        
        // Process entities in batches
        EntityFilterResult result = processBatchEntities(entityIds, request, entityType, startTime);
        
        logger.info("Batch entity filtering completed: processed={}, matched={}, batches={}", 
                   result.getTotalProcessed(), result.getTotalMatched(),
                   result.getMetrics() != null ? result.getMetrics().getBatchesProcessed() : 0);
        
        return result;
    }

    /**
     * Get total count of entities for a specific type
     */
    @Transactional(readOnly = true)
    public Long getEntityCount(String entityType) {
        logger.debug("Getting entity count for type: {}", entityType);
        
        EntityTypeEntity entityTypeEntity = validateEntityType(entityType);
        
        try {
            // This would need to be implemented based on the data service
            // For now, returning a placeholder implementation
            return 1000L; // Placeholder - would query the actual data service
            
        } catch (Exception e) {
            throw new DataServiceException("Failed to get entity count for type: " + entityType, e);
        }
    }

    /**
     * Validate that a rule is compatible with an entity type
     */
    @Transactional(readOnly = true)
    public boolean validateRuleForEntityType(String entityType, RuleDefinition rule) {
        logger.debug("Validating rule for entity type: {}", entityType);
        
        EntityTypeEntity entityTypeEntity = validateEntityType(entityType);
        
        try {
            // Extract field names from rule
            Set<String> ruleFields = extractFieldNamesFromRule(rule);
            
            // Get available fields for entity type (this would need to be implemented)
            Set<String> availableFields = getAvailableFieldsForEntityType(entityTypeEntity);
            
            // Check if all rule fields are available
            boolean isValid = availableFields.containsAll(ruleFields);
            
            if (!isValid) {
                Set<String> missingFields = new HashSet<>(ruleFields);
                missingFields.removeAll(availableFields);
                logger.warn("Rule validation failed for entity type {}: missing fields {}", 
                           entityType, missingFields);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Failed to validate rule for entity type: {}", entityType, e);
            return false;
        }
    }

    // Private helper methods

    private EntityTypeEntity validateEntityType(String entityType) {
        return entityTypeRepository.findByTypeNameActive(entityType)
                .orElseThrow(() -> new RuleNotFoundException("Entity type not found: " + entityType));
    }

    private List<String> getEntityIdsToProcess(EntityFilterRequest request, EntityTypeEntity entityType) {
        if (request.getEntityIds() != null && !request.getEntityIds().isEmpty()) {
            logger.debug("Using provided entity IDs: {}", request.getEntityIds().size());
            return new ArrayList<>(request.getEntityIds());
        } else {
            logger.debug("Querying all entity IDs for type: {}", entityType.getTypeName());
            return queryAllEntityIds(entityType);
        }
    }

    private List<String> queryAllEntityIds(EntityTypeEntity entityType) {
        try {
            // This would need to be implemented to query the data service for all entity IDs
            // For now, returning a placeholder implementation that simulates realistic entity IDs
            List<String> entityIds = new ArrayList<>();
            
            // Generate entity IDs based on entity type name for more realistic testing
            String entityPrefix = entityType.getTypeName().toLowerCase();
            int entityCount = 1000; // Configurable in real implementation
            
            for (int i = 1; i <= entityCount; i++) {
                entityIds.add(entityPrefix + "_" + i);
            }
            
            logger.debug("Generated {} placeholder entity IDs for type: {}", entityIds.size(), entityType.getTypeName());
            return entityIds;
            
        } catch (Exception e) {
            throw new DataServiceException("Failed to query entity IDs for type: " + entityType.getTypeName(), e);
        }
    }

    private List<String> applyPagination(List<String> entityIds, int page, int size) {
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, entityIds.size());
        
        if (startIndex >= entityIds.size()) {
            return new ArrayList<>();
        }
        
        return entityIds.subList(startIndex, endIndex);
    }

    private EntityFilterResult processEntities(List<String> entityIds, EntityFilterRequest request, 
                                             EntityTypeEntity entityType, long startTime) {
        
        List<FilteredEntity> filteredEntities = new ArrayList<>();
        List<EntityProcessingError> errors = new ArrayList<>();
        
        long dataRetrievalTime = 0;
        long ruleEvaluationTime = 0;
        long totalMatched = 0;
        long totalFailed = 0;

        for (String entityId : entityIds) {
            try {
                // Retrieve entity data
                long dataStart = System.currentTimeMillis();
                Map<String, Object> entityData = entityTypeService.retrieveEntityData(
                    entityType.getTypeName(), entityId);
                dataRetrievalTime += System.currentTimeMillis() - dataStart;

                // Create execution context
                ExecutionContext context = new ExecutionContext();
                context.setEntityId(entityId);
                context.setEntityType(entityType.getTypeName());
                context.setFieldValues(entityData);
                if (request.getContext() != null) {
                    context.getMetadata().putAll(request.getContext());
                }

                // Evaluate rule
                long ruleStart = System.currentTimeMillis();
                RuleExecutionResult ruleResult = ruleExecutionService.executeRuleWithDefinition(
                    request.getRule(), context);
                ruleEvaluationTime += System.currentTimeMillis() - ruleStart;

                // Create filtered entity
                FilteredEntity filteredEntity = new FilteredEntity(entityId, ruleResult.isResult());
                
                if (request.getIncludeEntityData()) {
                    filteredEntity.setEntityData(entityData);
                }
                
                if (request.getIncludeTrace()) {
                    // Convert traces to Map format
                    Map<String, Object> traceMap = new HashMap<>();
                    if (ruleResult.getTraces() != null) {
                        for (int i = 0; i < ruleResult.getTraces().size(); i++) {
                            RuleExecutionResult.ExecutionTrace trace = ruleResult.getTraces().get(i);
                            traceMap.put("trace_" + i, Map.of(
                                "path", trace.getPath(),
                                "description", trace.getDescription(),
                                "result", trace.isResult(),
                                "actualValue", trace.getActualValue(),
                                "expectedValue", trace.getExpectedValue()
                            ));
                        }
                    }
                    filteredEntity.setTrace(traceMap);
                }

                filteredEntities.add(filteredEntity);
                
                if (ruleResult.isResult()) {
                    totalMatched++;
                }

            } catch (Exception e) {
                logger.warn("Failed to process entity {}: {}", entityId, e.getMessage());
                
                FilteredEntity failedEntity = new FilteredEntity(entityId, false);
                failedEntity.setError(e.getMessage());
                filteredEntities.add(failedEntity);
                
                errors.add(new EntityProcessingError(entityId, e.getMessage(), "PROCESSING_ERROR"));
                totalFailed++;
            }
        }

        // Create result
        EntityFilterResult result = new EntityFilterResult();
        result.setEntities(filteredEntities);
        result.setTotalProcessed((long) entityIds.size());
        result.setTotalMatched(totalMatched);
        result.setTotalFailed(totalFailed);
        result.setErrors(errors);

        // Set metrics
        FilteringMetrics metrics = new FilteringMetrics();
        metrics.setDataRetrievalTimeMs(dataRetrievalTime);
        metrics.setRuleEvaluationTimeMs(ruleEvaluationTime);
        if (!entityIds.isEmpty()) {
            metrics.setAvgProcessingTimePerEntityMs(
                (double) (System.currentTimeMillis() - startTime) / entityIds.size());
        }
        metrics.setBatchesProcessed(1);
        result.setMetrics(metrics);

        return result;
    }

    private EntityFilterResult processBatchEntities(List<String> entityIds, EntityFilterRequest request, 
                                                   EntityTypeEntity entityType, long startTime) {
        
        List<FilteredEntity> allFilteredEntities = new ArrayList<>();
        List<EntityProcessingError> allErrors = new ArrayList<>();
        
        long totalDataRetrievalTime = 0;
        long totalRuleEvaluationTime = 0;
        long totalMatched = 0;
        long totalFailed = 0;
        int batchesProcessed = 0;

        // Process entities in batches
        int batchSize = request.getBatchSize();
        for (int i = 0; i < entityIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, entityIds.size());
            List<String> batchEntityIds = entityIds.subList(i, endIndex);
            
            logger.debug("Processing batch {}/{}: {} entities", 
                        batchesProcessed + 1, 
                        (int) Math.ceil((double) entityIds.size() / batchSize),
                        batchEntityIds.size());

            try {
                // Process batch
                EntityFilterResult batchResult = processBatch(batchEntityIds, request, entityType);
                
                // Aggregate results
                allFilteredEntities.addAll(batchResult.getEntities());
                if (batchResult.getErrors() != null) {
                    allErrors.addAll(batchResult.getErrors());
                }
                
                totalMatched += batchResult.getTotalMatched();
                totalFailed += batchResult.getTotalFailed();
                
                if (batchResult.getMetrics() != null) {
                    totalDataRetrievalTime += batchResult.getMetrics().getDataRetrievalTimeMs();
                    totalRuleEvaluationTime += batchResult.getMetrics().getRuleEvaluationTimeMs();
                }
                
                batchesProcessed++;
                
            } catch (Exception e) {
                logger.error("Failed to process batch starting at index {}: {}", i, e.getMessage());
                
                // Mark all entities in this batch as failed
                for (String entityId : batchEntityIds) {
                    FilteredEntity failedEntity = new FilteredEntity(entityId, false);
                    failedEntity.setError("Batch processing failed: " + e.getMessage());
                    allFilteredEntities.add(failedEntity);
                    
                    allErrors.add(new EntityProcessingError(entityId, e.getMessage(), "BATCH_ERROR"));
                }
                
                totalFailed += batchEntityIds.size();
                batchesProcessed++;
            }
        }

        // Create result
        EntityFilterResult result = new EntityFilterResult();
        result.setEntities(allFilteredEntities);
        result.setTotalProcessed((long) entityIds.size());
        result.setTotalMatched(totalMatched);
        result.setTotalFailed(totalFailed);
        result.setErrors(allErrors);

        // Set metrics
        FilteringMetrics metrics = new FilteringMetrics();
        metrics.setDataRetrievalTimeMs(totalDataRetrievalTime);
        metrics.setRuleEvaluationTimeMs(totalRuleEvaluationTime);
        if (!entityIds.isEmpty()) {
            metrics.setAvgProcessingTimePerEntityMs(
                (double) (System.currentTimeMillis() - startTime) / entityIds.size());
        }
        metrics.setBatchesProcessed(batchesProcessed);
        result.setMetrics(metrics);

        return result;
    }

    private EntityFilterResult processBatch(List<String> batchEntityIds, EntityFilterRequest request, 
                                          EntityTypeEntity entityType) {
        
        // For now, process sequentially within batch
        // This could be enhanced with parallel processing using CompletableFuture
        return processEntities(batchEntityIds, request, entityType, System.currentTimeMillis());
    }

    private Set<String> extractFieldNamesFromRule(RuleDefinition rule) {
        Set<String> fieldNames = new HashSet<>();
        
        if (rule.getRules() != null) {
            extractFieldNamesFromRuleItems(rule.getRules(), fieldNames);
        }
        
        return fieldNames;
    }

    private void extractFieldNamesFromRuleItems(List<com.rulesengine.model.RuleItem> items, Set<String> fieldNames) {
        if (items == null) {
            return;
        }
        
        for (com.rulesengine.model.RuleItem item : items) {
            if (item.isCondition() && item.getField() != null) {
                fieldNames.add(item.getField());
            } else if (item.isGroup() && item.getRules() != null) {
                extractFieldNamesFromRuleItems(item.getRules(), fieldNames);
            }
        }
    }

    private Set<String> getAvailableFieldsForEntityType(EntityTypeEntity entityType) {
        try {
            // This would need to be implemented to get available fields from field configurations
            // In a real implementation, this would query the FieldConfigRepository for fields
            // associated with this entity type and return their field names
            
            // For now, returning a more comprehensive placeholder set based on entity type
            Set<String> availableFields = new HashSet<>();
            
            // Common fields for all entity types
            availableFields.add("id");
            availableFields.add("createdAt");
            availableFields.add("updatedAt");
            availableFields.add("status");
            
            // Entity type specific fields
            String typeName = entityType.getTypeName().toLowerCase();
            switch (typeName) {
                case "user":
                    availableFields.addAll(Set.of("name", "email", "age", "department", "role", "active"));
                    break;
                case "order":
                    availableFields.addAll(Set.of("amount", "currency", "customerId", "orderDate", "items"));
                    break;
                case "product":
                    availableFields.addAll(Set.of("name", "price", "category", "inStock", "description"));
                    break;
                default:
                    // Generic fields for unknown entity types
                    availableFields.addAll(Set.of("name", "description", "value", "type"));
                    break;
            }
            
            logger.debug("Retrieved {} available fields for entity type: {}", availableFields.size(), entityType.getTypeName());
            return availableFields;
            
        } catch (Exception e) {
            logger.error("Failed to get available fields for entity type: {}", entityType.getTypeName(), e);
            // Return a minimal set of fields as fallback
            return Set.of("id", "status", "createdAt", "updatedAt");
        }
    }
}