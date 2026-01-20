package com.rulesengine.service;

import com.rulesengine.dto.BatchExecutionRequest;
import com.rulesengine.dto.ExecutionContext;
import com.rulesengine.dto.RuleExecutionResult;
import com.rulesengine.engine.RuleEvaluator;
import com.rulesengine.entity.RuleEntity;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for orchestrating rule execution with field resolution and dependency management
 */
@Service
@Transactional
public class RuleExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleExecutionService.class);
    
    private final RuleRepository ruleRepository;
    private final RuleEvaluator ruleEvaluator;
    private final FieldResolutionService fieldResolutionService;
    
    @Autowired
    public RuleExecutionService(RuleRepository ruleRepository, 
                               RuleEvaluator ruleEvaluator,
                               FieldResolutionService fieldResolutionService) {
        this.ruleRepository = ruleRepository;
        this.ruleEvaluator = ruleEvaluator;
        this.fieldResolutionService = fieldResolutionService;
    }
    
    /**
     * Execute a single rule against the provided execution context
     */
    @Transactional(readOnly = true)
    public RuleExecutionResult executeRule(Long ruleId, ExecutionContext context) {
        logger.info("Executing rule with ID: {} for entity: {}", ruleId, context.getEntityId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Find the rule
            RuleEntity rule = ruleRepository.findByIdActive(ruleId)
                    .orElseThrow(() -> new RuleNotFoundException(ruleId));
            
            // Parse rule definition
            RuleDefinition ruleDefinition = ruleEvaluator.parseRuleDefinition(rule.getRuleDefinitionJson());
            
            // Create execution result
            RuleExecutionResult result = new RuleExecutionResult(ruleId, rule.getName(), false);
            result.setExecutionTime(LocalDateTime.now());
            
            // Resolve field values (for now, use static values from context)
            Map<String, Object> fieldValues = resolveFieldValues(ruleDefinition, context);
            
            // Execute rule with traces
            RuleExecutionResult evaluationResult = ruleEvaluator.evaluateWithTrace(ruleDefinition, fieldValues);
            
            // Merge results
            result.setResult(evaluationResult.isResult());
            result.setTraces(evaluationResult.getTraces());
            result.setFieldValues(fieldValues);
            result.setHasError(evaluationResult.isHasError());
            result.setErrorMessage(evaluationResult.getErrorMessage());
            
            long endTime = System.currentTimeMillis();
            result.setExecutionDurationMs(endTime - startTime);
            
            logger.info("Rule {} executed in {}ms with result: {}", 
                rule.getName(), result.getExecutionDurationMs(), result.isResult());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing rule {}: {}", ruleId, e.getMessage(), e);
            
            RuleExecutionResult errorResult = new RuleExecutionResult(ruleId, "Unknown", false);
            errorResult.setError("Execution error: " + e.getMessage());
            errorResult.setExecutionDurationMs(System.currentTimeMillis() - startTime);
            
            return errorResult;
        }
    }
    
    /**
     * Execute multiple rules in batch against the same execution context
     */
    @Transactional(readOnly = true)
    public List<RuleExecutionResult> executeBatch(BatchExecutionRequest request) {
        logger.info("Executing batch of {} rules for entity: {}", 
            request.getRuleIds().size(), request.getContext().getEntityId());
        
        List<RuleExecutionResult> results = new ArrayList<>();
        
        for (Long ruleId : request.getRuleIds()) {
            try {
                RuleExecutionResult result = executeRule(ruleId, request.getContext());
                
                // Remove traces if not requested
                if (!request.isIncludeTraces()) {
                    result.setTraces(new ArrayList<>());
                }
                
                results.add(result);
                
                // Stop on first failure if requested
                if (request.isStopOnFirstFailure() && !result.isResult() && !result.isHasError()) {
                    logger.info("Stopping batch execution on first failure for rule: {}", ruleId);
                    break;
                }
                
            } catch (Exception e) {
                logger.error("Error executing rule {} in batch: {}", ruleId, e.getMessage(), e);
                
                RuleExecutionResult errorResult = new RuleExecutionResult(ruleId, "Unknown", false);
                errorResult.setError("Batch execution error: " + e.getMessage());
                results.add(errorResult);
                
                if (request.isStopOnFirstFailure()) {
                    logger.info("Stopping batch execution due to error for rule: {}", ruleId);
                    break;
                }
            }
        }
        
        logger.info("Batch execution completed. {} rules executed, {} successful", 
            results.size(), results.stream().mapToInt(r -> r.isResult() && !r.isHasError() ? 1 : 0).sum());
        
        return results;
    }
    
    /**
     * Execute a rule definition directly without persisting to database
     */
    @Transactional(readOnly = true)
    public RuleExecutionResult executeRuleWithDefinition(RuleDefinition ruleDefinition, ExecutionContext context) {
        logger.debug("Executing rule definition directly");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Create execution result
            RuleExecutionResult result = new RuleExecutionResult(-1L, "Direct Rule", false);
            result.setExecutionTime(LocalDateTime.now());
            
            // Resolve field values
            Map<String, Object> fieldValues = resolveFieldValues(ruleDefinition, context);
            
            // Execute rule with traces
            RuleExecutionResult evaluationResult = ruleEvaluator.evaluateWithTrace(ruleDefinition, fieldValues);
            
            // Merge results
            result.setResult(evaluationResult.isResult());
            result.setTraces(evaluationResult.getTraces());
            result.setFieldValues(fieldValues);
            result.setHasError(evaluationResult.isHasError());
            result.setErrorMessage(evaluationResult.getErrorMessage());
            
            long endTime = System.currentTimeMillis();
            result.setExecutionDurationMs(endTime - startTime);
            
            logger.debug("Rule definition executed in {}ms with result: {}", 
                result.getExecutionDurationMs(), result.isResult());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing rule definition: {}", e.getMessage(), e);
            
            RuleExecutionResult errorResult = new RuleExecutionResult(-1L, "Direct Rule", false);
            errorResult.setError("Execution error: " + e.getMessage());
            errorResult.setExecutionDurationMs(System.currentTimeMillis() - startTime);
            
            return errorResult;
        }
    }

    /**
     * Validate rule execution without persisting results
     */
    @Transactional(readOnly = true)
    public RuleExecutionResult validateRuleExecution(String ruleDefinitionJson, ExecutionContext context) {
        logger.debug("Validating rule execution for test rule");
        
        try {
            // Parse rule definition
            RuleDefinition ruleDefinition = ruleEvaluator.parseRuleDefinition(ruleDefinitionJson);
            
            // Resolve field values
            Map<String, Object> fieldValues = resolveFieldValues(ruleDefinition, context);
            
            // Execute rule with traces
            RuleExecutionResult result = ruleEvaluator.evaluateWithTrace(ruleDefinition, fieldValues);
            result.setRuleId(-1L); // Indicate test rule
            result.setRuleName("Test Rule");
            result.setFieldValues(fieldValues);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error validating rule execution: {}", e.getMessage(), e);
            
            RuleExecutionResult errorResult = new RuleExecutionResult(-1L, "Test Rule", false);
            errorResult.setError("Validation error: " + e.getMessage());
            
            return errorResult;
        }
    }
    
    /**
     * Resolve field values for rule execution using dynamic field resolution
     */
    private Map<String, Object> resolveFieldValues(RuleDefinition ruleDefinition, ExecutionContext context) {
        logger.debug("Resolving field values for rule execution using dynamic field resolution");
        
        try {
            // Extract field names from rule definition
            List<String> requiredFields = fieldResolutionService.extractFieldNamesFromRule(ruleDefinition);
            
            if (requiredFields.isEmpty()) {
                logger.debug("No fields required for rule execution, using context values");
                return context.getFieldValues();
            }
            
            logger.debug("Rule requires {} fields: {}", requiredFields.size(), requiredFields);
            
            // Use field resolution service to resolve field values dynamically
            Map<String, Object> resolvedValues = fieldResolutionService.resolveFields(requiredFields, context);
            
            // Merge with any additional context values that weren't resolved
            Map<String, Object> allFieldValues = new HashMap<>(context.getFieldValues());
            allFieldValues.putAll(resolvedValues);
            
            logger.debug("Resolved {} field values for rule execution", allFieldValues.size());
            
            return allFieldValues;
            
        } catch (Exception e) {
            logger.warn("Error in dynamic field resolution, falling back to context values: {}", e.getMessage());
            
            // Fallback to static values from context
            return context.getFieldValues();
        }
    }
    
    /**
     * Extract field names referenced in a rule definition
     * This will be used in future tasks for dependency analysis
     */
    private List<String> extractFieldNames(RuleDefinition ruleDefinition) {
        List<String> fieldNames = new ArrayList<>();
        
        if (ruleDefinition != null && ruleDefinition.getRules() != null) {
            extractFieldNamesFromItems(ruleDefinition.getRules(), fieldNames);
        }
        
        return fieldNames;
    }
    
    /**
     * Recursively extract field names from rule items
     */
    private void extractFieldNamesFromItems(List<com.rulesengine.model.RuleItem> items, List<String> fieldNames) {
        if (items == null) {
            return;
        }
        
        for (com.rulesengine.model.RuleItem item : items) {
            if (item.isCondition() && item.getField() != null) {
                if (!fieldNames.contains(item.getField())) {
                    fieldNames.add(item.getField());
                }
            } else if (item.isGroup() && item.getRules() != null) {
                extractFieldNamesFromItems(item.getRules(), fieldNames);
            }
        }
    }
}