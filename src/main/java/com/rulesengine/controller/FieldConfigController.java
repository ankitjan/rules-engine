package com.rulesengine.controller;

import com.rulesengine.dto.BuilderFieldConfigResponse;
import com.rulesengine.dto.CreateFieldConfigRequest;
import com.rulesengine.dto.FieldConfigResponse;
import com.rulesengine.dto.UpdateFieldConfigRequest;
import com.rulesengine.service.FieldConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequestMapping("/api/field-configs")
@Tag(name = "Field Configuration Management", description = "APIs for managing field configurations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FieldConfigController {

    private static final Logger logger = LoggerFactory.getLogger(FieldConfigController.class);

    private final FieldConfigService fieldConfigService;

    @Autowired
    public FieldConfigController(FieldConfigService fieldConfigService) {
        this.fieldConfigService = fieldConfigService;
    }

    @Operation(summary = "Create a new field configuration", description = "Creates a new field configuration with validation and data service support")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Field configuration created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid field configuration data"),
            @ApiResponse(responseCode = "409", description = "Field configuration with same name already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<FieldConfigResponse> createFieldConfig(
            @Valid @RequestBody CreateFieldConfigRequest request) {
        logger.info("Creating field configuration: {}", request.getFieldName());
        
        FieldConfigResponse response = fieldConfigService.createFieldConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get field configurations for Rule Builder", 
               description = "Retrieves field configurations optimized for Rule Builder UI with operators and validation rules")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field configurations for builder retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/for-builder")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<BuilderFieldConfigResponse>> getFieldConfigsForBuilder() {
        logger.debug("Getting field configurations optimized for Rule Builder UI");
        
        List<BuilderFieldConfigResponse> fieldConfigs = fieldConfigService.getFieldConfigsForBuilder();
        return ResponseEntity.ok(fieldConfigs);
    }

    @Operation(summary = "Get all field configurations", description = "Retrieves all active field configurations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field configurations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<FieldConfigResponse>> getFieldConfigs() {
        logger.debug("Getting all field configurations");
        
        List<FieldConfigResponse> fieldConfigs = fieldConfigService.getFieldConfigs();
        return ResponseEntity.ok(fieldConfigs);
    }

    @Operation(summary = "Get field configuration by ID", description = "Retrieves a specific field configuration by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field configuration retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Field configuration not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<FieldConfigResponse> getFieldConfig(
            @Parameter(description = "Field configuration ID") @PathVariable Long id) {
        logger.debug("Getting field configuration with ID: {}", id);
        
        FieldConfigResponse fieldConfig = fieldConfigService.getFieldConfig(id);
        return ResponseEntity.ok(fieldConfig);
    }

    @Operation(summary = "Get field configuration by name", description = "Retrieves a specific field configuration by its field name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field configuration retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Field configuration not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/by-name/{fieldName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<FieldConfigResponse> getFieldConfigByName(
            @Parameter(description = "Field name") @PathVariable String fieldName) {
        logger.debug("Getting field configuration with name: {}", fieldName);
        
        FieldConfigResponse fieldConfig = fieldConfigService.getFieldConfigByName(fieldName);
        return ResponseEntity.ok(fieldConfig);
    }

    @Operation(summary = "Update field configuration", description = "Updates an existing field configuration with versioning support")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid field configuration data"),
            @ApiResponse(responseCode = "404", description = "Field configuration not found"),
            @ApiResponse(responseCode = "409", description = "Field configuration with same name already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<FieldConfigResponse> updateFieldConfig(
            @Parameter(description = "Field configuration ID") @PathVariable Long id,
            @Valid @RequestBody UpdateFieldConfigRequest request) {
        logger.info("Updating field configuration with ID: {}", id);
        
        FieldConfigResponse response = fieldConfigService.updateFieldConfig(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete field configuration", description = "Soft deletes a field configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Field configuration deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Field configuration not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFieldConfig(
            @Parameter(description = "Field configuration ID") @PathVariable Long id) {
        logger.info("Deleting field configuration with ID: {}", id);
        
        fieldConfigService.deleteFieldConfig(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get field configurations by type", description = "Retrieves field configurations filtered by field type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field configurations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/by-type/{fieldType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<FieldConfigResponse>> getFieldConfigsByType(
            @Parameter(description = "Field type (STRING, NUMBER, DATE, BOOLEAN, ARRAY, OBJECT)") @PathVariable String fieldType) {
        logger.debug("Getting field configurations with type: {}", fieldType);
        
        List<FieldConfigResponse> fieldConfigs = fieldConfigService.getFieldConfigsByType(fieldType);
        return ResponseEntity.ok(fieldConfigs);
    }

    @Operation(summary = "Get calculated field configurations", description = "Retrieves all calculated field configurations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Calculated field configurations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/calculated")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<FieldConfigResponse>> getCalculatedFieldConfigs() {
        logger.debug("Getting calculated field configurations");
        
        List<FieldConfigResponse> fieldConfigs = fieldConfigService.getCalculatedFieldConfigs();
        return ResponseEntity.ok(fieldConfigs);
    }

    @Operation(summary = "Get field configurations with data services", description = "Retrieves field configurations that have data service configurations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field configurations with data services retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/with-data-service")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<FieldConfigResponse>> getFieldConfigsWithDataService() {
        logger.debug("Getting field configurations with data services");
        
        List<FieldConfigResponse> fieldConfigs = fieldConfigService.getFieldConfigsWithDataService();
        return ResponseEntity.ok(fieldConfigs);
    }
}