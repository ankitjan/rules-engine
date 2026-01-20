package com.rulesengine.controller;

import com.rulesengine.dto.EntityFilterRequest;
import com.rulesengine.dto.EntityFilterResult;
import com.rulesengine.service.EntityFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entities")
@Tag(name = "Entity Filtering", description = "Entity filtering and rule evaluation operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EntityController {

    private static final Logger logger = LoggerFactory.getLogger(EntityController.class);

    private final EntityFilterService entityFilterService;

    @Autowired
    public EntityController(EntityFilterService entityFilterService) {
        this.entityFilterService = entityFilterService;
    }

    @PostMapping("/filter")
    @Operation(
        summary = "Filter entities using a rule", 
        description = "Filters a collection of entities by applying a rule definition. " +
                     "Supports batch processing, pagination, and performance metrics."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Entities filtered successfully",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = EntityFilterResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request data - malformed rule definition or invalid parameters"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Entity type not found or not configured"
        ),
        @ApiResponse(
            responseCode = "502", 
            description = "Data service error - failed to retrieve entity data"
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Forbidden - insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error during entity filtering"
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('RULE_EXECUTOR')")
    public ResponseEntity<EntityFilterResult> filterEntities(
            @Parameter(description = "Entity filtering request with rule definition and parameters", required = true)
            @Valid @RequestBody EntityFilterRequest request) {
        
        logger.info("Filtering entities for type: {} with rule, page: {}, size: {}", 
                   request.getEntityType(), request.getPage(), request.getSize());
        
        if (request.getEntityIds() != null && !request.getEntityIds().isEmpty()) {
            logger.debug("Filtering {} specific entity IDs", request.getEntityIds().size());
        } else {
            logger.debug("Filtering all entities of type: {}", request.getEntityType());
        }

        try {
            long startTime = System.currentTimeMillis();
            
            EntityFilterResult result = entityFilterService.filterEntities(request);
            
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(executionTime);
            
            logger.info("Entity filtering completed: processed={}, matched={}, failed={}, time={}ms", 
                       result.getTotalProcessed(), result.getTotalMatched(), 
                       result.getTotalFailed(), executionTime);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to filter entities for type: {}", request.getEntityType(), e);
            throw e;
        }
    }

    @PostMapping("/filter/batch")
    @Operation(
        summary = "Filter entities in batch mode", 
        description = "Filters large collections of entities using optimized batch processing. " +
                     "Provides better performance for large datasets with configurable batch sizes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Batch filtering completed successfully",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = EntityFilterResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid batch request parameters"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Entity type not found"
        ),
        @ApiResponse(
            responseCode = "502", 
            description = "Data service error during batch processing"
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Forbidden"
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('RULE_EXECUTOR')")
    public ResponseEntity<EntityFilterResult> filterEntitiesBatch(
            @Parameter(description = "Batch entity filtering request", required = true)
            @Valid @RequestBody EntityFilterRequest request) {
        
        logger.info("Batch filtering entities for type: {} with batch size: {}", 
                   request.getEntityType(), request.getBatchSize());
        
        try {
            long startTime = System.currentTimeMillis();
            
            EntityFilterResult result = entityFilterService.filterEntitiesBatch(request);
            
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(executionTime);
            
            logger.info("Batch entity filtering completed: processed={}, matched={}, failed={}, batches={}, time={}ms", 
                       result.getTotalProcessed(), result.getTotalMatched(), result.getTotalFailed(),
                       result.getMetrics() != null ? result.getMetrics().getBatchesProcessed() : 0, 
                       executionTime);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to batch filter entities for type: {}", request.getEntityType(), e);
            throw e;
        }
    }

    @GetMapping("/{entityType}/count")
    @Operation(
        summary = "Count entities of a specific type", 
        description = "Returns the total count of entities available for a specific entity type"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Entity count retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Entity type not found"
        ),
        @ApiResponse(
            responseCode = "502", 
            description = "Data service error"
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Forbidden"
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('RULE_EXECUTOR')")
    public ResponseEntity<Long> getEntityCount(
            @Parameter(description = "Entity type name", required = true)
            @PathVariable String entityType) {
        
        logger.debug("Getting entity count for type: {}", entityType);
        
        try {
            Long count = entityFilterService.getEntityCount(entityType);
            logger.debug("Entity count for type {}: {}", entityType, count);
            return ResponseEntity.ok(count);
            
        } catch (Exception e) {
            logger.error("Failed to get entity count for type: {}", entityType, e);
            throw e;
        }
    }

    @PostMapping("/validate-rule")
    @Operation(
        summary = "Validate rule against entity type", 
        description = "Validates that a rule definition is compatible with the specified entity type's fields"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Rule validation completed"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Rule validation failed"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Entity type not found"
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Forbidden"
        )
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('RULE_EXECUTOR')")
    public ResponseEntity<Boolean> validateRuleForEntityType(
            @Parameter(description = "Entity type name", required = true)
            @RequestParam String entityType,
            @Parameter(description = "Rule definition to validate", required = true)
            @Valid @RequestBody EntityFilterRequest request) {
        
        logger.debug("Validating rule for entity type: {}", entityType);
        
        try {
            boolean isValid = entityFilterService.validateRuleForEntityType(entityType, request.getRule());
            logger.debug("Rule validation for entity type {}: {}", entityType, isValid ? "valid" : "invalid");
            return ResponseEntity.ok(isValid);
            
        } catch (Exception e) {
            logger.error("Failed to validate rule for entity type: {}", entityType, e);
            throw e;
        }
    }
}