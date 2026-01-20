package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.Map;

@Schema(description = "Request for searching field values with pagination and filtering")
public class FieldValueSearchRequest {

    @Schema(description = "Field name to search values for", example = "user_status", required = true)
    @JsonProperty("fieldName")
    @NotBlank(message = "Field name is required")
    private String fieldName;

    @Schema(description = "Search query to filter values", example = "active")
    @JsonProperty("query")
    private String query;

    @Schema(description = "Page number (0-based)", example = "0")
    @JsonProperty("page")
    @Min(value = 0, message = "Page number must be non-negative")
    private Integer page = 0;

    @Schema(description = "Page size", example = "20")
    @JsonProperty("size")
    @Min(value = 1, message = "Page size must be positive")
    private Integer size = 20;

    @Schema(description = "Sort field", example = "value")
    @JsonProperty("sortBy")
    private String sortBy = "value";

    @Schema(description = "Sort direction", example = "asc", allowableValues = {"asc", "desc"})
    @JsonProperty("sortDirection")
    private String sortDirection = "asc";

    @Schema(description = "Additional filters to apply")
    @JsonProperty("filters")
    private Map<String, Object> filters;

    @Schema(description = "Include inactive/archived values")
    @JsonProperty("includeInactive")
    private Boolean includeInactive = false;

    @Schema(description = "Specific value types to include")
    @JsonProperty("valueTypes")
    private List<String> valueTypes;

    @Schema(description = "Context for value retrieval (e.g., entity ID, tenant)")
    @JsonProperty("context")
    private Map<String, Object> context;

    // Constructors
    public FieldValueSearchRequest() {}

    public FieldValueSearchRequest(String fieldName, String query, Integer page, Integer size) {
        this.fieldName = fieldName;
        this.query = query;
        this.page = page;
        this.size = size;
    }

    // Getters and Setters
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }

    public Boolean getIncludeInactive() { return includeInactive; }
    public void setIncludeInactive(Boolean includeInactive) { this.includeInactive = includeInactive; }

    public List<String> getValueTypes() { return valueTypes; }
    public void setValueTypes(List<String> valueTypes) { this.valueTypes = valueTypes; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}