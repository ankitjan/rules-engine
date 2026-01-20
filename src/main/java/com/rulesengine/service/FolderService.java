package com.rulesengine.service;

import com.rulesengine.dto.CreateFolderRequest;
import com.rulesengine.dto.FolderResponse;
import com.rulesengine.dto.UpdateFolderRequest;
import com.rulesengine.entity.FolderEntity;
import com.rulesengine.entity.RuleEntity;
import com.rulesengine.exception.DuplicateRuleNameException;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.repository.FolderRepository;
import com.rulesengine.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
@Transactional
public class FolderService {

    private static final Logger logger = LoggerFactory.getLogger(FolderService.class);

    private final FolderRepository folderRepository;
    private final RuleRepository ruleRepository;

    @Autowired
    public FolderService(FolderRepository folderRepository, RuleRepository ruleRepository) {
        this.folderRepository = folderRepository;
        this.ruleRepository = ruleRepository;
    }

    /**
     * Create a new folder
     */
    public FolderEntity createFolder(CreateFolderRequest request) {
        logger.info("Creating folder with name: {}", request.getName());

        // Validate parent folder if specified
        FolderEntity parent = null;
        if (request.getParentId() != null) {
            parent = folderRepository.findByIdActive(request.getParentId())
                    .orElseThrow(() -> new RuleNotFoundException("Parent folder not found with ID: " + request.getParentId()));
        }

        // Check for duplicate names at the same level
        validateFolderNameUniqueness(request.getName(), parent);

        // Create the folder
        FolderEntity folder = new FolderEntity(request.getName(), request.getDescription(), parent);
        
        // Save to get the ID, then update the path
        folder = folderRepository.save(folder);
        folder.updatePath();
        folder = folderRepository.save(folder);

        // Add to parent if specified
        if (parent != null) {
            parent.addChild(folder);
            folderRepository.save(parent);
        }

        logger.info("Created folder with ID: {} and path: {}", folder.getId(), folder.getPath());
        return folder;
    }

    /**
     * Get all folders in hierarchy
     */
    @Transactional(readOnly = true)
    public List<FolderEntity> getAllFolders() {
        logger.debug("Retrieving all folders");
        return folderRepository.findAllActive();
    }

    /**
     * Get folder hierarchy starting from root folders
     */
    @Transactional(readOnly = true)
    public List<FolderEntity> getFolderHierarchy() {
        logger.debug("Retrieving folder hierarchy");
        return folderRepository.findRootFoldersActive();
    }

    /**
     * Get folder by ID
     */
    @Transactional(readOnly = true)
    public FolderEntity getFolder(Long id) {
        logger.debug("Retrieving folder with ID: {}", id);
        return folderRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException("Folder not found with ID: " + id));
    }

    /**
     * Update folder
     */
    public FolderEntity updateFolder(Long id, UpdateFolderRequest request) {
        logger.info("Updating folder with ID: {}", id);

        FolderEntity folder = getFolder(id);
        
        // Validate parent folder if specified and different from current
        FolderEntity newParent = null;
        if (request.getParentId() != null) {
            if (!request.getParentId().equals(folder.getParent() != null ? folder.getParent().getId() : null)) {
                newParent = folderRepository.findByIdActive(request.getParentId())
                        .orElseThrow(() -> new RuleNotFoundException("Parent folder not found with ID: " + request.getParentId()));
                
                // Prevent circular references
                validateNoCircularReference(folder, newParent);
            }
        }

        // Check for duplicate names if name is changing or parent is changing
        if (!request.getName().equals(folder.getName()) || newParent != null) {
            FolderEntity parentForValidation = newParent != null ? newParent : folder.getParent();
            validateFolderNameUniqueness(request.getName(), parentForValidation, folder.getId());
        }

        // Update folder properties
        folder.setName(request.getName());
        folder.setDescription(request.getDescription());

        // Update parent if changed
        if (newParent != null) {
            // Remove from old parent
            if (folder.getParent() != null) {
                folder.getParent().removeChild(folder);
            }
            
            // Add to new parent
            newParent.addChild(folder);
            folder.setParent(newParent);
            
            // Update paths for this folder and all descendants
            updateFolderPaths(folder);
        }

        folder = folderRepository.save(folder);
        logger.info("Updated folder with ID: {}", folder.getId());
        return folder;
    }

    /**
     * Delete folder with specified strategy
     */
    public void deleteFolder(Long id, FolderDeletionStrategy strategy) {
        logger.info("Deleting folder with ID: {} using strategy: {}", id, strategy);

        FolderEntity folder = getFolder(id);

        switch (strategy) {
            case MOVE_TO_PARENT:
                deleteFolderMoveToParent(folder);
                break;
            case RECURSIVE_DELETE:
                deleteFolderRecursive(folder);
                break;
            default:
                throw new IllegalArgumentException("Unknown deletion strategy: " + strategy);
        }

        logger.info("Deleted folder with ID: {}", id);
    }

    /**
     * Get children of a folder
     */
    @Transactional(readOnly = true)
    public List<FolderEntity> getFolderChildren(Long parentId) {
        logger.debug("Retrieving children of folder with ID: {}", parentId);
        return folderRepository.findByParentIdActive(parentId);
    }

    /**
     * Get folder path from root to specified folder
     */
    @Transactional(readOnly = true)
    public List<FolderEntity> getFolderPath(Long folderId) {
        logger.debug("Retrieving path for folder with ID: {}", folderId);
        
        FolderEntity folder = getFolder(folderId);
        String path = folder.getPath();
        
        if (path == null || path.isEmpty()) {
            return List.of(folder);
        }

        // Parse path to get folder IDs
        String[] pathParts = path.split("/");
        List<Long> folderIds = new java.util.ArrayList<>();
        
        for (String part : pathParts) {
            if (!part.isEmpty()) {
                try {
                    folderIds.add(Long.parseLong(part));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid folder ID in path: {}", part);
                }
            }
        }

        // Retrieve folders in order
        return folderIds.stream()
                .map(this::getFolder)
                .collect(java.util.stream.Collectors.toList());
    }

    // Private helper methods

    private void validateFolderNameUniqueness(String name, FolderEntity parent) {
        validateFolderNameUniqueness(name, parent, null);
    }

    private void validateFolderNameUniqueness(String name, FolderEntity parent, Long excludeId) {
        boolean exists;
        if (parent == null) {
            exists = folderRepository.existsByNameAtRootActive(name);
        } else {
            exists = folderRepository.existsByNameAndParentIdActive(name, parent.getId());
        }

        if (exists) {
            // If we're updating, check if the existing folder is the same one we're updating
            if (excludeId != null) {
                List<FolderEntity> siblings = parent == null ? 
                    folderRepository.findRootFoldersActive() : 
                    folderRepository.findByParentIdActive(parent.getId());
                
                boolean isDuplicate = siblings.stream()
                    .anyMatch(f -> f.getName().equals(name) && !f.getId().equals(excludeId));
                
                if (!isDuplicate) {
                    return; // Not a duplicate, it's the same folder being updated
                }
            }
            
            String location = parent == null ? "root level" : "folder '" + parent.getName() + "'";
            throw new DuplicateRuleNameException("Folder with name '" + name + "' already exists in " + location);
        }
    }

    private void validateNoCircularReference(FolderEntity folder, FolderEntity newParent) {
        if (newParent == null) {
            return;
        }

        // Check if newParent is a descendant of folder
        Set<Long> visited = new HashSet<>();
        FolderEntity current = newParent;
        
        while (current != null) {
            if (current.getId().equals(folder.getId())) {
                throw new IllegalArgumentException("Cannot move folder: would create circular reference");
            }
            
            if (visited.contains(current.getId())) {
                logger.warn("Detected circular reference in folder hierarchy at folder ID: {}", current.getId());
                break;
            }
            
            visited.add(current.getId());
            current = current.getParent();
        }
    }

    private void deleteFolderMoveToParent(FolderEntity folder) {
        FolderEntity parent = folder.getParent();

        // Move all children to parent
        for (FolderEntity child : folder.getChildren()) {
            child.setParent(parent);
            if (parent != null) {
                parent.addChild(child);
            }
            updateFolderPaths(child);
            folderRepository.save(child);
        }

        // Move all rules to parent
        for (RuleEntity rule : folder.getRules()) {
            rule.setFolder(parent);
            if (parent != null) {
                parent.addRule(rule);
            }
            ruleRepository.save(rule);
        }

        // Remove folder from parent
        if (parent != null) {
            parent.removeChild(folder);
            folderRepository.save(parent);
        }

        // Soft delete the folder
        folder.setIsDeleted(true);
        folderRepository.save(folder);
    }

    private void deleteFolderRecursive(FolderEntity folder) {
        // Recursively delete all children
        for (FolderEntity child : folder.getChildren()) {
            deleteFolderRecursive(child);
        }

        // Soft delete all rules in this folder
        for (RuleEntity rule : folder.getRules()) {
            rule.setIsDeleted(true);
            ruleRepository.save(rule);
        }

        // Remove folder from parent
        if (folder.getParent() != null) {
            folder.getParent().removeChild(folder);
            folderRepository.save(folder.getParent());
        }

        // Soft delete the folder
        folder.setIsDeleted(true);
        folderRepository.save(folder);
    }

    private void updateFolderPaths(FolderEntity folder) {
        folder.updatePath();
        folderRepository.save(folder);

        // Recursively update paths for all descendants
        for (FolderEntity child : folder.getChildren()) {
            updateFolderPaths(child);
        }
    }

    /**
     * Folder deletion strategies
     */
    public enum FolderDeletionStrategy {
        MOVE_TO_PARENT,
        RECURSIVE_DELETE
    }
}