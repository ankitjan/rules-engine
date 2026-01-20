package com.rulesengine.controller;

import com.rulesengine.dto.BatchExecutionRequest;
import com.rulesengine.dto.CreateRuleRequest;
import com.rulesengine.dto.ExecutionContext;
import com.rulesengine.dto.PagedRuleResponse;
import com.rulesengine.dto.RuleExecutionResult;
import com.rulesengine.dto.RuleResponse;
import com.rulesengine.dto.RuleValidationRequest;
import com.rulesengine.dto.RuleValidationResult;
import com.rulesengine.dto.UpdateRuleRequest;
import com.rulesengine.service.RuleExecutionService;
import com.rulesengine.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@Tag(name = "Rule Management", description = "APIs for managing business rules")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RuleController {

    private static final Logger logger = LoggerFactory.getLogger(RuleController.class);

    private final RuleService ruleService;
    private final RuleExecutionService ruleExecutionService;

    @Autowired
    public RuleController(RuleService ruleService, RuleExecutionService ruleExecutionService) {
        this.ruleService = ruleService;
        this.ruleExecutionService = ruleExecutionService;
    }

    @PostMapping
    @Operation(summary = "Create a new rule", description = "Creates a new business rule with the provided definition")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Rule created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid rule data or validation error"),
        @ApiResponse(responseCode = "409", description = "Rule with the same name already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RuleResponse> createRule(
            @Valid @RequestBody CreateRuleRequest request) {
        
        logger.info("Received request to create rule: {}", request.getName());
        
        RuleResponse response = ruleService.createRule(request);
        
        logger.info("Successfully created rule with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all rules", description = "Retrieves all rules with optional filtering and pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rules retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PagedRuleResponse> getRules(
            @Parameter(description = "Filter rules by name (case-insensitive partial match)")
            @RequestParam(required = false) String filter,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "name") String sortBy,
            
            @Parameter(description = "Sort direction (asc or desc)")
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        logger.debug("Received request to get rules with filter: {}, page: {}, size: {}", filter, page, size);
        
        // Create sort object
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PagedRuleResponse response = ruleService.getRules(filter, pageable);
        
        logger.debug("Successfully retrieved {} rules", response.getContent().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get rule by ID", description = "Retrieves a specific rule by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Rule not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RuleResponse> getRule(
            @Parameter(description = "Rule ID")
            @PathVariable Long id) {
        
        logger.debug("Received request to get rule with ID: {}", id);
        
        RuleResponse response = ruleService.getRule(id);
        
        logger.debug("Successfully retrieved rule: {}", response.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update rule", description = "Updates an existing rule with the provided data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid rule data or validation error"),
        @ApiResponse(responseCode = "404", description = "Rule not found"),
        @ApiResponse(responseCode = "409", description = "Rule with the same name already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RuleResponse> updateRule(
            @Parameter(description = "Rule ID")
            @PathVariable Long id,
            
            @Valid @RequestBody UpdateRuleRequest request) {
        
        logger.info("Received request to update rule with ID: {}", id);
        
        RuleResponse response = ruleService.updateRule(id, request);
        
        logger.info("Successfully updated rule with ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete rule", description = "Deletes a rule (soft delete)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Rule deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Rule not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteRule(
            @Parameter(description = "Rule ID")
            @PathVariable Long id) {
        
        logger.info("Received request to delete rule with ID: {}", id);
        
        ruleService.deleteRule(id);
        
        logger.info("Successfully deleted rule with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate rule structure", description = "Validates a rule definition without persisting it")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule is valid"),
        @ApiResponse(responseCode = "400", description = "Rule validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> validateRule(
            @Parameter(description = "Rule definition JSON to validate")
            @RequestBody String ruleDefinitionJson) {
        
        logger.debug("Received request to validate rule definition");
        
        try {
            ruleService.validateRuleStructure(ruleDefinitionJson);
            return ResponseEntity.ok("Rule definition is valid");
        } catch (Exception e) {
            logger.warn("Rule validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Validation failed: " + e.getMessage());
        }
    }

    @PostMapping("/validate-enhanced")
    @Operation(summary = "Enhanced rule validation for Rule Builder", 
               description = "Validates a rule definition with detailed error information and suggestions for Rule Builder UI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule validation completed"),
        @ApiResponse(responseCode = "400", description = "Invalid validation request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RuleValidationResult> validateRuleEnhanced(
            @Parameter(description = "Enhanced rule validation request")
            @Valid @RequestBody RuleValidationRequest request) {
        
        logger.debug("Received request for enhanced rule validation");
        
        RuleValidationResult result = ruleService.validateRuleForBuilder(request);
        
        logger.debug("Enhanced rule validation completed with {} errors", 
                    result.getErrors() != null ? result.getErrors().size() : 0);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute rule", description = "Executes a specific rule against provided data context")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule executed successfully"),
        @ApiResponse(responseCode = "404", description = "Rule not found"),
        @ApiResponse(responseCode = "400", description = "Invalid execution context"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RuleExecutionResult> executeRule(
            @Parameter(description = "Rule ID")
            @PathVariable Long id,
            
            @Parameter(description = "Execution context with field values")
            @Valid @RequestBody ExecutionContext context) {
        
        logger.info("Received request to execute rule with ID: {}", id);
        
        RuleExecutionResult result = ruleExecutionService.executeRule(id, context);
        
        logger.info("Rule execution completed with result: {}", result.isResult());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/execute-batch")
    @Operation(summary = "Execute multiple rules", description = "Executes multiple rules against the same data context")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch execution completed"),
        @ApiResponse(responseCode = "400", description = "Invalid batch execution request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<RuleExecutionResult>> executeBatch(
            @Parameter(description = "Batch execution request with rule IDs and context")
            @Valid @RequestBody BatchExecutionRequest request) {
        
        logger.info("Received request to execute batch of {} rules", request.getRuleIds().size());
        
        List<RuleExecutionResult> results = ruleExecutionService.executeBatch(request);
        
        logger.info("Batch execution completed with {} results", results.size());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/validate-execution")
    @Operation(summary = "Validate rule execution", description = "Tests rule execution without persisting the rule")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule execution validation completed"),
        @ApiResponse(responseCode = "400", description = "Invalid rule or execution context"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RuleExecutionResult> validateRuleExecution(
            @Parameter(description = "Rule definition JSON to test")
            @RequestParam String ruleDefinitionJson,
            
            @Parameter(description = "Execution context with field values")
            @Valid @RequestBody ExecutionContext context) {
        
        logger.debug("Received request to validate rule execution");
        
        RuleExecutionResult result = ruleExecutionService.validateRuleExecution(ruleDefinitionJson, context);
        
        logger.debug("Rule execution validation completed with result: {}", result.isResult());
        return ResponseEntity.ok(result);
    }
}