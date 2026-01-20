package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Result of entity filtering operation")
public class EntityFilterResult {

    @Schema(description = "List of filtered entities")
    @JsonProperty("entities")
    private List<FilteredEntity> entities;

    @Schema(description = "Total number of entities processed")
    @JsonProperty("totalProcessed")
    private Long totalProcessed;

    @Schema(description = "Total number of entities that matched the rule")
    @JsonProperty("totalMatched")
    private Long totalMatched;

    @Schema(description = "Number of entities that failed processing")
    @JsonProperty("totalFailed")
    private Long totalFailed;

    @Schema(description = "Execution time in milliseconds")
    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;

    @Schema(description = "Timestamp when filtering was executed")
    @JsonProperty("executedAt")
    private LocalDateTime executedAt;

    @Schema(description = "Pagination information")
    @JsonProperty("pagination")
    private PaginationInfo pagination;

    @Schema(description = "Performance metrics")
    @JsonProperty("metrics")
    private FilteringMetrics metrics;

    @Schema(description = "List of errors encountered during processing")
    @JsonProperty("errors")
    private List<EntityProcessingError> errors;

    // Constructors
    public EntityFilterResult() {
        this.executedAt = LocalDateTime.now();
    }

    public EntityFilterResult(List<FilteredEntity> entities, Long totalProcessed, Long totalMatched) {
        this();
        this.entities = entities;
        this.totalProcessed = totalProcessed;
        this.totalMatched = totalMatched;
        this.totalFailed = 0L;
    }

    // Nested classes
    @Schema(description = "Information about a filtered entity")
    public static class FilteredEntity {
        @Schema(description = "Entity ID")
        @JsonProperty("entityId")
        private String entityId;

        @Schema(description = "Whether the entity matched the rule")
        @JsonProperty("matched")
        private Boolean matched;

        @Schema(description = "Entity data (if requested)")
        @JsonProperty("entityData")
        private Map<String, Object> entityData;

        @Schema(description = "Rule execution trace (if requested)")
        @JsonProperty("trace")
        private Map<String, Object> trace;

        @Schema(description = "Error message if processing failed")
        @JsonProperty("error")
        private String error;

        // Constructors
        public FilteredEntity() {}

        public FilteredEntity(String entityId, Boolean matched) {
            this.entityId = entityId;
            this.matched = matched;
        }

        public FilteredEntity(String entityId, Boolean matched, Map<String, Object> entityData) {
            this.entityId = entityId;
            this.matched = matched;
            this.entityData = entityData;
        }

        // Getters and Setters
        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public Boolean getMatched() {
            return matched;
        }

        public void setMatched(Boolean matched) {
            this.matched = matched;
        }

        public Map<String, Object> getEntityData() {
            return entityData;
        }

        public void setEntityData(Map<String, Object> entityData) {
            this.entityData = entityData;
        }

        public Map<String, Object> getTrace() {
            return trace;
        }

        public void setTrace(Map<String, Object> trace) {
            this.trace = trace;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    @Schema(description = "Pagination information")
    public static class PaginationInfo {
        @Schema(description = "Current page number (0-based)")
        @JsonProperty("page")
        private Integer page;

        @Schema(description = "Page size")
        @JsonProperty("size")
        private Integer size;

        @Schema(description = "Total number of pages")
        @JsonProperty("totalPages")
        private Integer totalPages;

        @Schema(description = "Total number of elements")
        @JsonProperty("totalElements")
        private Long totalElements;

        @Schema(description = "Whether this is the first page")
        @JsonProperty("first")
        private Boolean first;

        @Schema(description = "Whether this is the last page")
        @JsonProperty("last")
        private Boolean last;

        // Constructors
        public PaginationInfo() {}

        public PaginationInfo(Integer page, Integer size, Long totalElements) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / size);
            this.first = page == 0;
            this.last = page >= totalPages - 1;
        }

        // Getters and Setters
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

        public Integer getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(Integer totalPages) {
            this.totalPages = totalPages;
        }

        public Long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(Long totalElements) {
            this.totalElements = totalElements;
        }

        public Boolean getFirst() {
            return first;
        }

        public void setFirst(Boolean first) {
            this.first = first;
        }

        public Boolean getLast() {
            return last;
        }

        public void setLast(Boolean last) {
            this.last = last;
        }
    }

    @Schema(description = "Performance metrics for filtering operation")
    public static class FilteringMetrics {
        @Schema(description = "Time spent retrieving entity data (ms)")
        @JsonProperty("dataRetrievalTimeMs")
        private Long dataRetrievalTimeMs;

        @Schema(description = "Time spent evaluating rules (ms)")
        @JsonProperty("ruleEvaluationTimeMs")
        private Long ruleEvaluationTimeMs;

        @Schema(description = "Average processing time per entity (ms)")
        @JsonProperty("avgProcessingTimePerEntityMs")
        private Double avgProcessingTimePerEntityMs;

        @Schema(description = "Number of batches processed")
        @JsonProperty("batchesProcessed")
        private Integer batchesProcessed;

        // Constructors
        public FilteringMetrics() {}

        // Getters and Setters
        public Long getDataRetrievalTimeMs() {
            return dataRetrievalTimeMs;
        }

        public void setDataRetrievalTimeMs(Long dataRetrievalTimeMs) {
            this.dataRetrievalTimeMs = dataRetrievalTimeMs;
        }

        public Long getRuleEvaluationTimeMs() {
            return ruleEvaluationTimeMs;
        }

        public void setRuleEvaluationTimeMs(Long ruleEvaluationTimeMs) {
            this.ruleEvaluationTimeMs = ruleEvaluationTimeMs;
        }

        public Double getAvgProcessingTimePerEntityMs() {
            return avgProcessingTimePerEntityMs;
        }

        public void setAvgProcessingTimePerEntityMs(Double avgProcessingTimePerEntityMs) {
            this.avgProcessingTimePerEntityMs = avgProcessingTimePerEntityMs;
        }

        public Integer getBatchesProcessed() {
            return batchesProcessed;
        }

        public void setBatchesProcessed(Integer batchesProcessed) {
            this.batchesProcessed = batchesProcessed;
        }
    }

    @Schema(description = "Error information for failed entity processing")
    public static class EntityProcessingError {
        @Schema(description = "Entity ID that failed processing")
        @JsonProperty("entityId")
        private String entityId;

        @Schema(description = "Error message")
        @JsonProperty("message")
        private String message;

        @Schema(description = "Error code")
        @JsonProperty("code")
        private String code;

        // Constructors
        public EntityProcessingError() {}

        public EntityProcessingError(String entityId, String message, String code) {
            this.entityId = entityId;
            this.message = message;
            this.code = code;
        }

        // Getters and Setters
        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    // Getters and Setters
    public List<FilteredEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<FilteredEntity> entities) {
        this.entities = entities;
    }

    public Long getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(Long totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public Long getTotalMatched() {
        return totalMatched;
    }

    public void setTotalMatched(Long totalMatched) {
        this.totalMatched = totalMatched;
    }

    public Long getTotalFailed() {
        return totalFailed;
    }

    public void setTotalFailed(Long totalFailed) {
        this.totalFailed = totalFailed;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public PaginationInfo getPagination() {
        return pagination;
    }

    public void setPagination(PaginationInfo pagination) {
        this.pagination = pagination;
    }

    public FilteringMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(FilteringMetrics metrics) {
        this.metrics = metrics;
    }

    public List<EntityProcessingError> getErrors() {
        return errors;
    }

    public void setErrors(List<EntityProcessingError> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "EntityFilterResult{" +
                "totalProcessed=" + totalProcessed +
                ", totalMatched=" + totalMatched +
                ", totalFailed=" + totalFailed +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}