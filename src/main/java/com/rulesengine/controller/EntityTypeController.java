package com.rulesengine.controller;

import com.rulesengine.dto.CreateEntityTypeRequest;
import com.rulesengine.dto.EntityTypeResponse;
import com.rulesengine.dto.UpdateEntityTypeRequest;
import com.rulesengine.service.EntityTypeService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entity-types")
@Tag(name = "Entity Types", description = "Entity type management operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EntityTypeController {

    private static final Logger logger = LoggerFactory.getLogger(EntityTypeController.class);

    private final EntityTypeService entityTypeService;

    @Autowired
    public EntityTypeController(EntityTypeService entityTypeService) {
        this.entityTypeService = entityTypeService;
    }

    @PostMapping
    @Operation(summary = "Create a new entity type", description = "Creates a new entity type with data service configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Entity type created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EntityTypeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Entity type with the same name already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENTITY_TYPE_MANAGER')")
    public ResponseEntity<EntityTypeResponse> createEntityType(
            @Valid @RequestBody CreateEntityTypeRequest request) {
        
        logger.info("Creating entity type: {}", request.getTypeName());
        
        try {
            EntityTypeResponse response = entityTypeService.createEntityType(request);
            logger.info("Successfully created entity type with ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Failed to create entity type: {}", request.getTypeName(), e);
            throw e;
        }
    }

    @GetMapping
    @Operation(summary = "Get all entity types", description = "Retrieves all active entity types")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entity types retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EntityTypeResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('ENTITY_TYPE_MANAGER')")
    public ResponseEntity<List<EntityTypeResponse>> getAllEntityTypes(
            @Parameter(description = "Include inheritance information")
            @RequestParam(value = "includeInheritance", defaultValue = "false") boolean includeInheritance) {
        
        logger.debug("Retrieving all entity types, includeInheritance: {}", includeInheritance);
        
        try {
            List<EntityTypeResponse> response = includeInheritance 
                ? entityTypeService.getEntityTypesWithInheritance()
                : entityTypeService.getAllEntityTypes();
            
            logger.debug("Retrieved {} entity types", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to retrieve entity types", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get entity type by ID", description = "Retrieves a specific entity type by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entity type retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EntityTypeResponse.class))),
        @ApiResponse(responseCode = "404", description = "Entity type not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('ENTITY_TYPE_MANAGER')")
    public ResponseEntity<EntityTypeResponse> getEntityType(
            @Parameter(description = "Entity type ID", required = true)
            @PathVariable Long id) {
        
        logger.debug("Retrieving entity type with ID: {}", id);
        
        try {
            EntityTypeResponse response = entityTypeService.getEntityType(id);
            logger.debug("Retrieved entity type: {}", response.getTypeName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to retrieve entity type with ID: {}", id, e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update entity type", description = "Updates an existing entity type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entity type updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EntityTypeResponse.class))),
        @ApiResponse(responseCode = "404", description = "Entity type not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENTITY_TYPE_MANAGER')")
    public ResponseEntity<EntityTypeResponse> updateEntityType(
            @Parameter(description = "Entity type ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateEntityTypeRequest request) {
        
        logger.info("Updating entity type with ID: {}", id);
        
        try {
            EntityTypeResponse response = entityTypeService.updateEntityType(id, request);
            logger.info("Successfully updated entity type with ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to update entity type with ID: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete entity type", description = "Soft deletes an entity type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Entity type deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Entity type not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENTITY_TYPE_MANAGER')")
    public ResponseEntity<Void> deleteEntityType(
            @Parameter(description = "Entity type ID", required = true)
            @PathVariable Long id) {
        
        logger.info("Deleting entity type with ID: {}", id);
        
        try {
            entityTypeService.deleteEntityType(id);
            logger.info("Successfully deleted entity type with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Failed to delete entity type with ID: {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate entity type configuration", 
              description = "Validates entity type configuration including data service connectivity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration is valid"),
        @ApiResponse(responseCode = "400", description = "Configuration validation failed"),
        @ApiResponse(responseCode = "404", description = "Entity type not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENTITY_TYPE_MANAGER')")
    public ResponseEntity<Map<String, Object>> validateEntityTypeConfiguration(
            @Parameter(description = "Entity type ID", required = true)
            @PathVariable Long id) {
        
        logger.debug("Validating configuration for entity type with ID: {}", id);
        
        try {
            // Get the entity type and validate its configuration
            EntityTypeResponse entityType = entityTypeService.getEntityType(id);
            
            // Create a validation request from the entity type
            CreateEntityTypeRequest validationRequest = new CreateEntityTypeRequest();
            validationRequest.setTypeName(entityType.getTypeName());
            validationRequest.setDescription(entityType.getDescription());
            validationRequest.setDataServiceConfig(entityType.getDataServiceConfig());
            validationRequest.setFieldMappings(entityType.getFieldMappings());
            validationRequest.setParentTypeName(entityType.getParentTypeName());
            validationRequest.setMetadata(entityType.getMetadata());
            
            entityTypeService.validateEntityTypeConfiguration(validationRequest);
            
            Map<String, Object> result = Map.of(
                "valid", true,
                "message", "Entity type configuration is valid"
            );
            
            logger.debug("Configuration validation passed for entity type with ID: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Configuration validation failed for entity type with ID: {}", id, e);
            
            Map<String, Object> result = Map.of(
                "valid", false,
                "message", e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/{typeName}/data/{entityId}")
    @Operation(summary = "Retrieve entity data", 
              description = "Retrieves entity data from the configured data service")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entity data retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Entity type or entity not found"),
        @ApiResponse(responseCode = "502", description = "Data service error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('ENTITY_TYPE_MANAGER')")
    public ResponseEntity<Map<String, Object>> retrieveEntityData(
            @Parameter(description = "Entity type name", required = true)
            @PathVariable String typeName,
            @Parameter(description = "Entity ID", required = true)
            @PathVariable String entityId) {
        
        logger.debug("Retrieving entity data for type: {} and ID: {}", typeName, entityId);
        
        try {
            Map<String, Object> entityData = entityTypeService.retrieveEntityData(typeName, entityId);
            logger.debug("Successfully retrieved entity data for type: {} and ID: {}", typeName, entityId);
            return ResponseEntity.ok(entityData);
        } catch (Exception e) {
            logger.error("Failed to retrieve entity data for type: {} and ID: {}", typeName, entityId, e);
            throw e;
        }
    }
}