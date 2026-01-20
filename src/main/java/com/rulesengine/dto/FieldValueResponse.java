package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Field value response with metadata")
public class FieldValueResponse {

    @Schema(description = "The actual field value")
    @JsonProperty("value")
    private Object value;

    @Schema(description = "Display label for the value")
    @JsonProperty("label")
    private String label;

    @Schema(description = "Value description or additional context")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Value type", example = "STRING")
    @JsonProperty("valueType")
    private String valueType;

    @Schema(description = "Whether this value is active/available")
    @JsonProperty("isActive")
    private Boolean isActive;

    @Schema(description = "Usage count or frequency")
    @JsonProperty("usageCount")
    private Long usageCount;

    @Schema(description = "Last used timestamp")
    @JsonProperty("lastUsed")
    private LocalDateTime lastUsed;

    @Schema(description = "Additional metadata for the value")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Source of the value (e.g., 'database', 'api', 'calculated')")
    @JsonProperty("source")
    private String source;

    @Schema(description = "Category or group for the value")
    @JsonProperty("category")
    private String category;

    @Schema(description = "Sort order for display")
    @JsonProperty("sortOrder")
    private Integer sortOrder;

    // Constructors
    public FieldValueResponse() {}

    public FieldValueResponse(Object value, String label) {
        this.value = value;
        this.label = label;
        this.isActive = true;
    }

    public FieldValueResponse(Object value, String label, String description, String valueType,
                            Boolean isActive, Long usageCount, LocalDateTime lastUsed,
                            Map<String, Object> metadata, String source, String category, Integer sortOrder) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.valueType = valueType;
        this.isActive = isActive;
        this.usageCount = usageCount;
        this.lastUsed = lastUsed;
        this.metadata = metadata;
        this.source = source;
        this.category = category;
        this.sortOrder = sortOrder;
    }

    // Getters and Setters
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }

    public LocalDateTime getLastUsed() { return lastUsed; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}