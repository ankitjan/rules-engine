package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response for field value search results")
public class PagedFieldValueResponse {

    @Schema(description = "List of field values")
    @JsonProperty("content")
    private List<FieldValueResponse> content;

    @Schema(description = "Current page number (0-based)")
    @JsonProperty("page")
    private Integer page;

    @Schema(description = "Page size")
    @JsonProperty("size")
    private Integer size;

    @Schema(description = "Total number of elements")
    @JsonProperty("totalElements")
    private Long totalElements;

    @Schema(description = "Total number of pages")
    @JsonProperty("totalPages")
    private Integer totalPages;

    @Schema(description = "Whether this is the first page")
    @JsonProperty("first")
    private Boolean first;

    @Schema(description = "Whether this is the last page")
    @JsonProperty("last")
    private Boolean last;

    @Schema(description = "Whether there are more elements")
    @JsonProperty("hasNext")
    private Boolean hasNext;

    @Schema(description = "Whether there are previous elements")
    @JsonProperty("hasPrevious")
    private Boolean hasPrevious;

    @Schema(description = "Field name that was searched")
    @JsonProperty("fieldName")
    private String fieldName;

    @Schema(description = "Search query that was applied")
    @JsonProperty("searchQuery")
    private String searchQuery;

    // Constructors
    public PagedFieldValueResponse() {}

    public PagedFieldValueResponse(List<FieldValueResponse> content, Integer page, Integer size,
                                 Long totalElements, Integer totalPages, Boolean first, Boolean last,
                                 Boolean hasNext, Boolean hasPrevious, String fieldName, String searchQuery) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.fieldName = fieldName;
        this.searchQuery = searchQuery;
    }

    // Getters and Setters
    public List<FieldValueResponse> getContent() { return content; }
    public void setContent(List<FieldValueResponse> content) { this.content = content; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public Long getTotalElements() { return totalElements; }
    public void setTotalElements(Long totalElements) { this.totalElements = totalElements; }

    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

    public Boolean getFirst() { return first; }
    public void setFirst(Boolean first) { this.first = first; }

    public Boolean getLast() { return last; }
    public void setLast(Boolean last) { this.last = last; }

    public Boolean getHasNext() { return hasNext; }
    public void setHasNext(Boolean hasNext) { this.hasNext = hasNext; }

    public Boolean getHasPrevious() { return hasPrevious; }
    public void setHasPrevious(Boolean hasPrevious) { this.hasPrevious = hasPrevious; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
}