package com.rulesengine.dto;

import com.rulesengine.entity.FolderEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class FolderResponse {

    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private String path;
    private int depth;
    private boolean hasChildren;
    private boolean hasRules;
    private int childrenCount;
    private int rulesCount;
    private List<FolderResponse> children;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Constructors
    public FolderResponse() {}

    public FolderResponse(FolderEntity entity) {
        this(entity, false);
    }

    public FolderResponse(FolderEntity entity, boolean includeChildren) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.parentId = entity.getParent() != null ? entity.getParent().getId() : null;
        this.parentName = entity.getParent() != null ? entity.getParent().getName() : null;
        this.path = entity.getPath();
        this.depth = entity.getDepth();
        this.hasChildren = entity.hasChildren();
        this.hasRules = entity.hasRules();
        this.childrenCount = entity.getChildren().size();
        this.rulesCount = entity.getRules().size();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.createdBy = entity.getCreatedBy();
        this.updatedBy = entity.getUpdatedBy();

        if (includeChildren && entity.hasChildren()) {
            this.children = entity.getChildren().stream()
                    .map(child -> new FolderResponse(child, false))
                    .collect(Collectors.toList());
        }
    }

    // Static factory methods
    public static FolderResponse from(FolderEntity entity) {
        return new FolderResponse(entity);
    }

    public static FolderResponse fromWithChildren(FolderEntity entity) {
        return new FolderResponse(entity, true);
    }

    public static List<FolderResponse> fromList(List<FolderEntity> entities) {
        return entities.stream()
                .map(FolderResponse::from)
                .collect(Collectors.toList());
    }

    public static List<FolderResponse> fromListWithChildren(List<FolderEntity> entities) {
        return entities.stream()
                .map(FolderResponse::fromWithChildren)
                .collect(Collectors.toList());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public boolean isHasRules() {
        return hasRules;
    }

    public void setHasRules(boolean hasRules) {
        this.hasRules = hasRules;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public int getRulesCount() {
        return rulesCount;
    }

    public void setRulesCount(int rulesCount) {
        this.rulesCount = rulesCount;
    }

    public List<FolderResponse> getChildren() {
        return children;
    }

    public void setChildren(List<FolderResponse> children) {
        this.children = children;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "FolderResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", depth=" + depth +
                ", hasChildren=" + hasChildren +
                ", hasRules=" + hasRules +
                ", childrenCount=" + childrenCount +
                ", rulesCount=" + rulesCount +
                '}';
    }
}