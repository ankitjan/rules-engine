package com.rulesengine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.CreateFolderRequest;
import com.rulesengine.dto.UpdateFolderRequest;
import com.rulesengine.entity.FolderEntity;
import com.rulesengine.exception.DuplicateRuleNameException;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.service.FolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FolderController.class)
class FolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FolderService folderService;

    @Autowired
    private ObjectMapper objectMapper;

    private FolderEntity testFolder;

    @BeforeEach
    void setUp() {
        testFolder = new FolderEntity("Test Folder", "Test Description");
        testFolder.setId(1L);
    }

    @Test
    void createFolder_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        CreateFolderRequest request = new CreateFolderRequest("New Folder", "New Description", null);
        when(folderService.createFolder(any(CreateFolderRequest.class))).thenReturn(testFolder);

        // When & Then
        mockMvc.perform(post("/api/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Folder"))
                .andExpect(jsonPath("$.description").value("Test Description"));

        verify(folderService).createFolder(any(CreateFolderRequest.class));
    }

    @Test
    void createFolder_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        CreateFolderRequest request = new CreateFolderRequest("", "Description", null); // Empty name

        // When & Then
        mockMvc.perform(post("/api/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(folderService, never()).createFolder(any(CreateFolderRequest.class));
    }

    @Test
    void createFolder_WithDuplicateName_ShouldReturnConflict() throws Exception {
        // Given
        CreateFolderRequest request = new CreateFolderRequest("Existing Folder", "Description", null);
        when(folderService.createFolder(any(CreateFolderRequest.class)))
                .thenThrow(new DuplicateRuleNameException("Folder with name 'Existing Folder' already exists"));

        // When & Then
        mockMvc.perform(post("/api/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(folderService).createFolder(any(CreateFolderRequest.class));
    }

    @Test
    void getFolders_ShouldReturnFolderHierarchy() throws Exception {
        // Given
        List<FolderEntity> folders = Arrays.asList(testFolder);
        when(folderService.getFolderHierarchy()).thenReturn(folders);

        // When & Then
        mockMvc.perform(get("/api/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Folder"));

        verify(folderService).getFolderHierarchy();
    }

    @Test
    void getFolder_WithValidId_ShouldReturnFolder() throws Exception {
        // Given
        when(folderService.getFolder(1L)).thenReturn(testFolder);

        // When & Then
        mockMvc.perform(get("/api/folders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Folder"))
                .andExpect(jsonPath("$.description").value("Test Description"));

        verify(folderService).getFolder(1L);
    }

    @Test
    void getFolder_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        when(folderService.getFolder(999L)).thenThrow(new RuleNotFoundException("Folder not found"));

        // When & Then
        mockMvc.perform(get("/api/folders/999"))
                .andExpect(status().isNotFound());

        verify(folderService).getFolder(999L);
    }

    @Test
    void getFolderChildren_ShouldReturnChildren() throws Exception {
        // Given
        FolderEntity child = new FolderEntity("Child Folder", "Child Description");
        child.setId(2L);
        List<FolderEntity> children = Arrays.asList(child);
        when(folderService.getFolderChildren(1L)).thenReturn(children);

        // When & Then
        mockMvc.perform(get("/api/folders/1/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Child Folder"));

        verify(folderService).getFolderChildren(1L);
    }

    @Test
    void getFolderPath_ShouldReturnPath() throws Exception {
        // Given
        List<FolderEntity> path = Arrays.asList(testFolder);
        when(folderService.getFolderPath(1L)).thenReturn(path);

        // When & Then
        mockMvc.perform(get("/api/folders/1/path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Folder"));

        verify(folderService).getFolderPath(1L);
    }

    @Test
    void updateFolder_WithValidRequest_ShouldReturnUpdatedFolder() throws Exception {
        // Given
        UpdateFolderRequest request = new UpdateFolderRequest("Updated Name", "Updated Description", null);
        FolderEntity updatedFolder = new FolderEntity("Updated Name", "Updated Description");
        updatedFolder.setId(1L);
        when(folderService.updateFolder(eq(1L), any(UpdateFolderRequest.class))).thenReturn(updatedFolder);

        // When & Then
        mockMvc.perform(put("/api/folders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated Description"));

        verify(folderService).updateFolder(eq(1L), any(UpdateFolderRequest.class));
    }

    @Test
    void deleteFolder_WithValidStrategy_ShouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(folderService).deleteFolder(eq(1L), any(FolderService.FolderDeletionStrategy.class));

        // When & Then
        mockMvc.perform(delete("/api/folders/1")
                .param("strategy", "MOVE_TO_PARENT"))
                .andExpect(status().isNoContent());

        verify(folderService).deleteFolder(1L, FolderService.FolderDeletionStrategy.MOVE_TO_PARENT);
    }

    @Test
    void deleteFolder_WithInvalidStrategy_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/folders/1")
                .param("strategy", "INVALID_STRATEGY"))
                .andExpect(status().isBadRequest());

        verify(folderService, never()).deleteFolder(anyLong(), any(FolderService.FolderDeletionStrategy.class));
    }

    @Test
    void deleteFolder_WithDefaultStrategy_ShouldUseDefaultStrategy() throws Exception {
        // Given
        doNothing().when(folderService).deleteFolder(eq(1L), any(FolderService.FolderDeletionStrategy.class));

        // When & Then
        mockMvc.perform(delete("/api/folders/1"))
                .andExpect(status().isNoContent());

        verify(folderService).deleteFolder(1L, FolderService.FolderDeletionStrategy.MOVE_TO_PARENT);
    }

    @Test
    void getAllFolders_ShouldReturnAllFolders() throws Exception {
        // Given
        List<FolderEntity> folders = Arrays.asList(testFolder);
        when(folderService.getAllFolders()).thenReturn(folders);

        // When & Then
        mockMvc.perform(get("/api/folders/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Folder"));

        verify(folderService).getAllFolders();
    }
}