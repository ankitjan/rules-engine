package com.rulesengine.integration;

import com.rulesengine.entity.FolderEntity;
import com.rulesengine.repository.FolderRepository;
import com.rulesengine.service.FolderService;
import com.rulesengine.dto.CreateFolderRequest;
import com.rulesengine.dto.UpdateFolderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FolderManagementIntegrationTest {

    @Autowired
    private FolderService folderService;

    @Autowired
    private FolderRepository folderRepository;

    @Test
    void testCompleteFolderManagementWorkflow() {
        // 1. Create root folder
        CreateFolderRequest rootRequest = new CreateFolderRequest("Root Folder", "Root Description", null);
        FolderEntity rootFolder = folderService.createFolder(rootRequest);
        
        assertThat(rootFolder).isNotNull();
        assertThat(rootFolder.getId()).isNotNull();
        assertThat(rootFolder.getName()).isEqualTo("Root Folder");
        assertThat(rootFolder.getPath()).isEqualTo("/" + rootFolder.getId());
        assertThat(rootFolder.getDepth()).isEqualTo(1);

        // 2. Create child folder
        CreateFolderRequest childRequest = new CreateFolderRequest("Child Folder", "Child Description", rootFolder.getId());
        FolderEntity childFolder = folderService.createFolder(childRequest);
        
        assertThat(childFolder).isNotNull();
        assertThat(childFolder.getParent()).isEqualTo(rootFolder);
        assertThat(childFolder.getPath()).isEqualTo("/" + rootFolder.getId() + "/" + childFolder.getId());
        assertThat(childFolder.getDepth()).isEqualTo(2);

        // 3. Create grandchild folder
        CreateFolderRequest grandchildRequest = new CreateFolderRequest("Grandchild Folder", "Grandchild Description", childFolder.getId());
        FolderEntity grandchildFolder = folderService.createFolder(grandchildRequest);
        
        assertThat(grandchildFolder).isNotNull();
        assertThat(grandchildFolder.getParent()).isEqualTo(childFolder);
        assertThat(grandchildFolder.getDepth()).isEqualTo(3);

        // 4. Test hierarchy retrieval
        List<FolderEntity> hierarchy = folderService.getFolderHierarchy();
        assertThat(hierarchy).hasSize(1); // Only root folders
        assertThat(hierarchy.get(0)).isEqualTo(rootFolder);

        // 5. Test children retrieval
        List<FolderEntity> children = folderService.getFolderChildren(rootFolder.getId());
        assertThat(children).hasSize(1);
        assertThat(children.get(0)).isEqualTo(childFolder);

        // 6. Test path retrieval
        List<FolderEntity> path = folderService.getFolderPath(grandchildFolder.getId());
        assertThat(path).hasSize(3);
        assertThat(path.get(0)).isEqualTo(rootFolder);
        assertThat(path.get(1)).isEqualTo(childFolder);
        assertThat(path.get(2)).isEqualTo(grandchildFolder);

        // 7. Test folder update
        UpdateFolderRequest updateRequest = new UpdateFolderRequest("Updated Child", "Updated Description", rootFolder.getId());
        FolderEntity updatedFolder = folderService.updateFolder(childFolder.getId(), updateRequest);
        
        assertThat(updatedFolder.getName()).isEqualTo("Updated Child");
        assertThat(updatedFolder.getDescription()).isEqualTo("Updated Description");

        // 8. Test folder deletion with MOVE_TO_PARENT strategy
        folderService.deleteFolder(childFolder.getId(), FolderService.FolderDeletionStrategy.MOVE_TO_PARENT);
        
        // Verify child folder is deleted
        FolderEntity deletedChild = folderRepository.findById(childFolder.getId()).orElse(null);
        assertThat(deletedChild).isNotNull();
        assertThat(deletedChild.getIsDeleted()).isTrue();

        // Verify grandchild was moved to root
        FolderEntity movedGrandchild = folderRepository.findById(grandchildFolder.getId()).orElse(null);
        assertThat(movedGrandchild).isNotNull();
        assertThat(movedGrandchild.getIsDeleted()).isFalse();
        assertThat(movedGrandchild.getParent()).isEqualTo(rootFolder);
    }

    @Test
    void testFolderDeletionWithRecursiveStrategy() {
        // Create folder hierarchy
        CreateFolderRequest rootRequest = new CreateFolderRequest("Root", "Root Description", null);
        FolderEntity rootFolder = folderService.createFolder(rootRequest);

        CreateFolderRequest childRequest = new CreateFolderRequest("Child", "Child Description", rootFolder.getId());
        FolderEntity childFolder = folderService.createFolder(childRequest);

        CreateFolderRequest grandchildRequest = new CreateFolderRequest("Grandchild", "Grandchild Description", childFolder.getId());
        FolderEntity grandchildFolder = folderService.createFolder(grandchildRequest);

        // Delete with recursive strategy
        folderService.deleteFolder(rootFolder.getId(), FolderService.FolderDeletionStrategy.RECURSIVE_DELETE);

        // Verify all folders are deleted
        FolderEntity deletedRoot = folderRepository.findById(rootFolder.getId()).orElse(null);
        FolderEntity deletedChild = folderRepository.findById(childFolder.getId()).orElse(null);
        FolderEntity deletedGrandchild = folderRepository.findById(grandchildFolder.getId()).orElse(null);

        assertThat(deletedRoot.getIsDeleted()).isTrue();
        assertThat(deletedChild.getIsDeleted()).isTrue();
        assertThat(deletedGrandchild.getIsDeleted()).isTrue();
    }

    @Test
    void testMaterializedPathEfficiency() {
        // Create a deep hierarchy
        FolderEntity current = null;
        for (int i = 1; i <= 5; i++) {
            CreateFolderRequest request = new CreateFolderRequest("Level " + i, "Description " + i, 
                current != null ? current.getId() : null);
            current = folderService.createFolder(request);
            
            assertThat(current.getDepth()).isEqualTo(i);
            assertThat(current.getPath()).contains("/" + current.getId());
        }

        // Test that we can efficiently query by path
        List<FolderEntity> allFolders = folderService.getAllFolders();
        assertThat(allFolders).hasSize(5);

        // Verify path structure
        FolderEntity deepestFolder = current;
        String[] pathParts = deepestFolder.getPath().split("/");
        assertThat(pathParts).hasSize(6); // Empty string + 5 folder IDs
    }
}