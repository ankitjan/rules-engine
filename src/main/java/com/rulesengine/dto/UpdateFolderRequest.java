package com.rulesengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateFolderRequest {

    @NotBlank(message = "Folder name is required")
    @Size(max = 255, message = "Folder name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Long parentId;

    // Constructors
    public UpdateFolderRequest() {}

    public UpdateFolderRequest(String name, String description, Long parentId) {
        this.name = name;
        this.description = description;
        this.parentId = parentId;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "UpdateFolderRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parentId=" + parentId +
                '}';
    }
}