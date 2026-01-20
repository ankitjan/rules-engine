package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rulesengine.model.RuleDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

@Schema(description = "Request to filter entities using a rule")
public class EntityFilterRequest {

    @Schema(description = "Entity type name", example = "User", required = true)
    @NotBlank(message = "Entity type is required")
    @JsonProperty("entityType")
    private String entityType;

    @Schema(description = "List of entity IDs to filter. If empty, all entities of the type will be queried", 
            example = "[\"user1\", \"user2\", \"user3\"]")
    @JsonProperty("entityIds")
    private List<String> entityIds;

    @Schema(description = "Rule definition to apply for filtering", required = true)
    @NotNull(message = "Rule definition is required")
    @Valid
    @JsonProperty("rule")
    private RuleDefinition rule;

    @Schema(description = "Additional context data for rule evaluation")
    @JsonProperty("context")
    private Map<String, Object> context;

    @Schema(description = "Batch size for processing large entity collections", 
            example = "100", minimum = "1", maximum = "1000")
    @Positive(message = "Batch size must be positive")
    @JsonProperty("batchSize")
    private Integer batchSize = 100;

    @Schema(description = "Page number for pagination (0-based)", example = "0", minimum = "0")
    @JsonProperty("page")
    private Integer page = 0;

    @Schema(description = "Page size for pagination", example = "20", minimum = "1", maximum = "1000")
    @Positive(message = "Page size must be positive")
    @JsonProperty("size")
    private Integer size = 20;

    @Schema(description = "Whether to include entity data in the response", example = "true")
    @JsonProperty("includeEntityData")
    private Boolean includeEntityData = false;

    @Schema(description = "Whether to include execution traces for debugging", example = "false")
    @JsonProperty("includeTrace")
    private Boolean includeTrace = false;

    // Constructors
    public EntityFilterRequest() {}

    public EntityFilterRequest(String entityType, RuleDefinition rule) {
        this.entityType = entityType;
        this.rule = rule;
    }

    // Getters and Setters
    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public List<String> getEntityIds() {
        return entityIds;
    }

    public void setEntityIds(List<String> entityIds) {
        this.entityIds = entityIds;
    }

    public RuleDefinition getRule() {
        return rule;
    }

    public void setRule(RuleDefinition rule) {
        this.rule = rule;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Boolean getIncludeEntityData() {
        return includeEntityData;
    }

    public void setIncludeEntityData(Boolean includeEntityData) {
        this.includeEntityData = includeEntityData;
    }

    public Boolean getIncludeTrace() {
        return includeTrace;
    }

    public void setIncludeTrace(Boolean includeTrace) {
        this.includeTrace = includeTrace;
    }

    @Override
    public String toString() {
        return "EntityFilterRequest{" +
                "entityType='" + entityType + '\'' +
                ", entityIds=" + (entityIds != null ? entityIds.size() + " entities" : "all entities") +
                ", rule=" + (rule != null ? "defined" : "null") +
                ", batchSize=" + batchSize +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}