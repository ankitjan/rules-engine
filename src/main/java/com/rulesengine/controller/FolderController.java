package com.rulesengine.controller;

import com.rulesengine.dto.CreateFolderRequest;
import com.rulesengine.dto.FolderResponse;
import com.rulesengine.dto.UpdateFolderRequest;
import com.rulesengine.entity.FolderEntity;
import com.rulesengine.service.FolderService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@Tag(name = "Folder Management", description = "APIs for managing folder hierarchy and organization")
public class FolderController {

    private static final Logger logger = LoggerFactory.getLogger(FolderController.class);

    private final FolderService folderService;

    @Autowired
    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PostMapping
    @Operation(summary = "Create a new folder", description = "Creates a new folder with optional parent folder")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Folder created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Parent folder not found"),
            @ApiResponse(responseCode = "409", description = "Folder name already exists at this level")
    })
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody CreateFolderRequest request) {
        
        logger.info("Creating folder: {}", request.getName());
        
        FolderEntity folder = folderService.createFolder(request);
        FolderResponse response = FolderResponse.from(folder);
        
        logger.info("Created folder with ID: {}", folder.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get folder hierarchy", description = "Retrieves the complete folder hierarchy starting from root folders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder hierarchy retrieved successfully")
    })
    public ResponseEntity<List<FolderResponse>> getFolders(
            @Parameter(description = "Include children in response")
            @RequestParam(defaultValue = "true") boolean includeChildren) {
        
        logger.debug("Retrieving folder hierarchy, includeChildren: {}", includeChildren);
        
        List<FolderEntity> folders = folderService.getFolderHierarchy();
        List<FolderResponse> response = includeChildren ? 
            FolderResponse.fromListWithChildren(folders) : 
            FolderResponse.fromList(folders);
        
        logger.debug("Retrieved {} root folders", folders.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get folder by ID", description = "Retrieves a specific folder by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    public ResponseEntity<FolderResponse> getFolder(
            @Parameter(description = "Folder ID") @PathVariable Long id,
            @Parameter(description = "Include children in response")
            @RequestParam(defaultValue = "false") boolean includeChildren) {
        
        logger.debug("Retrieving folder with ID: {}", id);
        
        FolderEntity folder = folderService.getFolder(id);
        FolderResponse response = includeChildren ? 
            FolderResponse.fromWithChildren(folder) : 
            FolderResponse.from(folder);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get folder children", description = "Retrieves direct children of a specific folder")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Children retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Parent folder not found")
    })
    public ResponseEntity<List<FolderResponse>> getFolderChildren(
            @Parameter(description = "Parent folder ID") @PathVariable Long id) {
        
        logger.debug("Retrieving children of folder with ID: {}", id);
        
        List<FolderEntity> children = folderService.getFolderChildren(id);
        List<FolderResponse> response = FolderResponse.fromList(children);
        
        logger.debug("Retrieved {} children for folder ID: {}", children.size(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/path")
    @Operation(summary = "Get folder path", description = "Retrieves the complete path from root to the specified folder")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder path retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    public ResponseEntity<List<FolderResponse>> getFolderPath(
            @Parameter(description = "Folder ID") @PathVariable Long id) {
        
        logger.debug("Retrieving path for folder with ID: {}", id);
        
        List<FolderEntity> path = folderService.getFolderPath(id);
        List<FolderResponse> response = FolderResponse.fromList(path);
        
        logger.debug("Retrieved path with {} folders for folder ID: {}", path.size(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update folder", description = "Updates folder properties including name, description, and parent")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Folder or parent folder not found"),
            @ApiResponse(responseCode = "409", description = "Folder name already exists at this level")
    })
    public ResponseEntity<FolderResponse> updateFolder(
            @Parameter(description = "Folder ID") @PathVariable Long id,
            @Valid @RequestBody UpdateFolderRequest request) {
        
        logger.info("Updating folder with ID: {}", id);
        
        FolderEntity folder = folderService.updateFolder(id, request);
        FolderResponse response = FolderResponse.from(folder);
        
        logger.info("Updated folder with ID: {}", folder.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete folder", description = "Deletes a folder using the specified deletion strategy")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Folder deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Folder not found"),
            @ApiResponse(responseCode = "400", description = "Invalid deletion strategy")
    })
    public ResponseEntity<Void> deleteFolder(
            @Parameter(description = "Folder ID") @PathVariable Long id,
            @Parameter(description = "Deletion strategy: MOVE_TO_PARENT or RECURSIVE_DELETE")
            @RequestParam(defaultValue = "MOVE_TO_PARENT") String strategy) {
        
        logger.info("Deleting folder with ID: {} using strategy: {}", id, strategy);
        
        FolderService.FolderDeletionStrategy deletionStrategy;
        try {
            deletionStrategy = FolderService.FolderDeletionStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid deletion strategy: {}", strategy);
            return ResponseEntity.badRequest().build();
        }
        
        folderService.deleteFolder(id, deletionStrategy);
        
        logger.info("Deleted folder with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    @Operation(summary = "Get all folders", description = "Retrieves all folders in a flat list (not hierarchical)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All folders retrieved successfully")
    })
    public ResponseEntity<List<FolderResponse>> getAllFolders() {
        
        logger.debug("Retrieving all folders");
        
        List<FolderEntity> folders = folderService.getAllFolders();
        List<FolderResponse> response = FolderResponse.fromList(folders);
        
        logger.debug("Retrieved {} folders", folders.size());
        return ResponseEntity.ok(response);
    }
}