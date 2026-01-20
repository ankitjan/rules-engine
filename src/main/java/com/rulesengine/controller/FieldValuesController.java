package com.rulesengine.controller;

import com.rulesengine.dto.FieldValueResponse;
import com.rulesengine.dto.FieldValueSearchRequest;
import com.rulesengine.dto.PagedFieldValueResponse;
import com.rulesengine.service.FieldValuesService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/field-values")
@Tag(name = "Field Values Management", description = "APIs for retrieving and searching field values for Rule Builder UI")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FieldValuesController {

    private static final Logger logger = LoggerFactory.getLogger(FieldValuesController.class);

    private final FieldValuesService fieldValuesService;

    @Autowired
    public FieldValuesController(FieldValuesService fieldValuesService) {
        this.fieldValuesService = fieldValuesService;
    }

    @Operation(summary = "Search field values", 
               description = "Search for field values with pagination, filtering, and sorting support for Rule Builder UI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field values retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search request"),
            @ApiResponse(responseCode = "404", description = "Field configuration not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<PagedFieldValueResponse> searchFieldValues(
            @Valid @RequestBody FieldValueSearchRequest request) {
        logger.debug("Searching field values for field: {} with query: {}", 
                    request.getFieldName(), request.getQuery());
        
        PagedFieldValueResponse response = fieldValuesService.searchFieldValues(request);
        
        logger.debug("Found {} field values for field: {}", 
                    response.getTotalElements(), request.getFieldName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all values for a field", 
               description = "Retrieve all available values for a specific field with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field values retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Field configuration not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{fieldName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<FieldValueResponse>> getFieldValues(
            @Parameter(description = "Field name to get values for") @PathVariable String fieldName,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "value") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Include inactive values") @RequestParam(defaultValue = "false") boolean includeInactive) {
        
        logger.debug("Getting field values for field: {} with pagination page: {}, size: {}", 
                    fieldName, page, size);
        
        // Create sort object
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        List<FieldValueResponse> fieldValues = fieldValuesService.getFieldValues(fieldName, pageable, includeInactive);
        
        logger.debug("Retrieved {} field values for field: {}", fieldValues.size(), fieldName);
        return ResponseEntity.ok(fieldValues);
    }

    @Operation(summary = "Get distinct values for a field", 
               description = "Retrieve distinct values for a field, useful for dropdown options")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Distinct field values retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Field configuration not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{fieldName}/distinct")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<FieldValueResponse>> getDistinctFieldValues(
            @Parameter(description = "Field name to get distinct values for") @PathVariable String fieldName,
            @Parameter(description = "Maximum number of values to return") @RequestParam(defaultValue = "100") int limit,
            @Parameter(description = "Include inactive values") @RequestParam(defaultValue = "false") boolean includeInactive) {
        
        logger.debug("Getting distinct field values for field: {} with limit: {}", fieldName, limit);
        
        List<FieldValueResponse> distinctValues = fieldValuesService.getDistinctFieldValues(fieldName, limit, includeInactive);
        
        logger.debug("Retrieved {} distinct field values for field: {}", distinctValues.size(), fieldName);
        return ResponseEntity.ok(distinctValues);
    }

    @Operation(summary = "Refresh field value cache", 
               description = "Refresh the cached values for a specific field")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field value cache refreshed successfully"),
            @ApiResponse(responseCode = "404", description = "Field configuration not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{fieldName}/refresh-cache")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<String> refreshFieldValueCache(
            @Parameter(description = "Field name to refresh cache for") @PathVariable String fieldName) {
        
        logger.info("Refreshing field value cache for field: {}", fieldName);
        
        fieldValuesService.refreshFieldValueCache(fieldName);
        
        logger.info("Successfully refreshed field value cache for field: {}", fieldName);
        return ResponseEntity.ok("Field value cache refreshed successfully for field: " + fieldName);
    }

    @Operation(summary = "Preload field values", 
               description = "Preload values for multiple fields into cache for improved performance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field values preloaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid field names"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/preload")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<String> preloadFieldValues(
            @Parameter(description = "List of field names to preload") @RequestBody List<String> fieldNames) {
        
        logger.info("Preloading field values for {} fields", fieldNames.size());
        
        fieldValuesService.preloadFieldValues(fieldNames);
        
        logger.info("Successfully preloaded field values for {} fields", fieldNames.size());
        return ResponseEntity.ok("Field values preloaded successfully for " + fieldNames.size() + " fields");
    }

    @Operation(summary = "Get field value statistics", 
               description = "Get usage statistics and metadata for field values")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Field value statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Field configuration not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{fieldName}/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Object> getFieldValueStatistics(
            @Parameter(description = "Field name to get statistics for") @PathVariable String fieldName) {
        
        logger.debug("Getting field value statistics for field: {}", fieldName);
        
        Object statistics = fieldValuesService.getFieldValueStatistics(fieldName);
        
        logger.debug("Retrieved field value statistics for field: {}", fieldName);
        return ResponseEntity.ok(statistics);
    }
}