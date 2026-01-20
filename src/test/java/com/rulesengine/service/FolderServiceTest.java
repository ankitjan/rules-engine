package com.rulesengine.service;

import com.rulesengine.dto.CreateFolderRequest;
import com.rulesengine.dto.UpdateFolderRequest;
import com.rulesengine.entity.FolderEntity;
import com.rulesengine.exception.DuplicateRuleNameException;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.repository.FolderRepository;
import com.rulesengine.repository.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private FolderService folderService;

    private FolderEntity testFolder;
    private FolderEntity parentFolder;

    @BeforeEach
    void setUp() {
        testFolder = new FolderEntity("Test Folder", "Test Description");
        testFolder.setId(1L);
        
        parentFolder = new FolderEntity("Parent Folder", "Parent Description");
        parentFolder.setId(2L);
    }

    @Test
    void createFolder_WithValidRequest_ShouldCreateFolder() {
        // Given
        CreateFolderRequest request = new CreateFolderRequest("New Folder", "New Description", null);
        FolderEntity savedFolder = new FolderEntity("New Folder", "New Description");
        savedFolder.setId(1L);

        when(folderRepository.existsByNameAtRootActive("New Folder")).thenReturn(false);
        when(folderRepository.save(any(FolderEntity.class))).thenReturn(savedFolder);

        // When
        FolderEntity result = folderService.createFolder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Folder");
        assertThat(result.getDescription()).isEqualTo("New Description");
        verify(folderRepository, times(2)).save(any(FolderEntity.class)); // Once for initial save, once for path update
    }

    @Test
    void createFolder_WithParent_ShouldCreateFolderWithParent() {
        // Given
        CreateFolderRequest request = new CreateFolderRequest("Child Folder", "Child Description", 2L);
        FolderEntity childFolder = new FolderEntity("Child Folder", "Child Description", parentFolder);
        childFolder.setId(3L);

        when(folderRepository.findByIdActive(2L)).thenReturn(Optional.of(parentFolder));
        when(folderRepository.existsByNameAndParentIdActive("Child Folder", 2L)).thenReturn(false);
        when(folderRepository.save(any(FolderEntity.class))).thenReturn(childFolder);

        // When
        FolderEntity result = folderService.createFolder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Child Folder");
        assertThat(result.getParent()).isEqualTo(parentFolder);
        verify(folderRepository, times(3)).save(any(FolderEntity.class)); // Child save, path update, parent save
    }

    @Test
    void createFolder_WithDuplicateName_ShouldThrowException() {
        // Given
        CreateFolderRequest request = new CreateFolderRequest("Existing Folder", "Description", null);
        when(folderRepository.existsByNameAtRootActive("Existing Folder")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> folderService.createFolder(request))
                .isInstanceOf(DuplicateRuleNameException.class)
                .hasMessageContaining("Folder with name 'Existing Folder' already exists");
    }

    @Test
    void createFolder_WithNonExistentParent_ShouldThrowException() {
        // Given
        CreateFolderRequest request = new CreateFolderRequest("Child Folder", "Description", 999L);
        when(folderRepository.findByIdActive(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> folderService.createFolder(request))
                .isInstanceOf(RuleNotFoundException.class)
                .hasMessageContaining("Parent folder not found with ID: 999");
    }

    @Test
    void getFolder_WithValidId_ShouldReturnFolder() {
        // Given
        when(folderRepository.findByIdActive(1L)).thenReturn(Optional.of(testFolder));

        // When
        FolderEntity result = folderService.getFolder(1L);

        // Then
        assertThat(result).isEqualTo(testFolder);
        verify(folderRepository).findByIdActive(1L);
    }

    @Test
    void getFolder_WithInvalidId_ShouldThrowException() {
        // Given
        when(folderRepository.findByIdActive(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> folderService.getFolder(999L))
                .isInstanceOf(RuleNotFoundException.class)
                .hasMessageContaining("Folder not found with ID: 999");
    }

    @Test
    void getAllFolders_ShouldReturnAllFolders() {
        // Given
        List<FolderEntity> folders = Arrays.asList(testFolder, parentFolder);
        when(folderRepository.findAllActive()).thenReturn(folders);

        // When
        List<FolderEntity> result = folderService.getAllFolders();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testFolder, parentFolder);
        verify(folderRepository).findAllActive();
    }

    @Test
    void getFolderHierarchy_ShouldReturnRootFolders() {
        // Given
        List<FolderEntity> rootFolders = Arrays.asList(testFolder);
        when(folderRepository.findRootFoldersActive()).thenReturn(rootFolders);

        // When
        List<FolderEntity> result = folderService.getFolderHierarchy();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testFolder);
        verify(folderRepository).findRootFoldersActive();
    }

    @Test
    void updateFolder_WithValidRequest_ShouldUpdateFolder() {
        // Given
        UpdateFolderRequest request = new UpdateFolderRequest("Updated Name", "Updated Description", null);
        when(folderRepository.findByIdActive(1L)).thenReturn(Optional.of(testFolder));
        when(folderRepository.existsByNameAtRootActive("Updated Name")).thenReturn(false);
        when(folderRepository.save(any(FolderEntity.class))).thenReturn(testFolder);

        // When
        FolderEntity result = folderService.updateFolder(1L, request);

        // Then
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        verify(folderRepository).save(testFolder);
    }

    @Test
    void updateFolder_WithCircularReference_ShouldThrowException() {
        // Given
        FolderEntity childFolder = new FolderEntity("Child", "Child Description", testFolder);
        childFolder.setId(3L);
        testFolder.addChild(childFolder);

        UpdateFolderRequest request = new UpdateFolderRequest("Updated Name", "Updated Description", 3L);
        
        when(folderRepository.findByIdActive(1L)).thenReturn(Optional.of(testFolder));
        when(folderRepository.findByIdActive(3L)).thenReturn(Optional.of(childFolder));

        // When & Then
        assertThatThrownBy(() -> folderService.updateFolder(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot move folder: would create circular reference");
    }

    @Test
    void getFolderChildren_ShouldReturnChildren() {
        // Given
        FolderEntity child1 = new FolderEntity("Child 1", "Description 1");
        FolderEntity child2 = new FolderEntity("Child 2", "Description 2");
        List<FolderEntity> children = Arrays.asList(child1, child2);
        
        when(folderRepository.findByParentIdActive(1L)).thenReturn(children);

        // When
        List<FolderEntity> result = folderService.getFolderChildren(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(child1, child2);
        verify(folderRepository).findByParentIdActive(1L);
    }

    @Test
    void deleteFolder_WithMoveToParentStrategy_ShouldMoveContentsToParent() {
        // Given
        testFolder.setParent(parentFolder);
        when(folderRepository.findByIdActive(1L)).thenReturn(Optional.of(testFolder));

        // When
        folderService.deleteFolder(1L, FolderService.FolderDeletionStrategy.MOVE_TO_PARENT);

        // Then
        verify(folderRepository, atLeastOnce()).save(any(FolderEntity.class));
        assertThat(testFolder.getIsDeleted()).isTrue();
    }

    @Test
    void deleteFolder_WithRecursiveStrategy_ShouldDeleteRecursively() {
        // Given
        when(folderRepository.findByIdActive(1L)).thenReturn(Optional.of(testFolder));

        // When
        folderService.deleteFolder(1L, FolderService.FolderDeletionStrategy.RECURSIVE_DELETE);

        // Then
        verify(folderRepository, atLeastOnce()).save(any(FolderEntity.class));
        assertThat(testFolder.getIsDeleted()).isTrue();
    }
}