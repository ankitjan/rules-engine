package com.rulesengine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.*;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.service.EntityTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EntityTypeController.class, 
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    })
@ContextConfiguration(classes = {EntityTypeController.class, com.rulesengine.exception.GlobalExceptionHandler.class})
class EntityTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntityTypeService entityTypeService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateEntityTypeRequest createRequest;
    private UpdateEntityTypeRequest updateRequest;
    private EntityTypeResponse entityTypeResponse;
    private DataServiceConfigDto dataServiceConfigDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        dataServiceConfigDto = new DataServiceConfigDto();
        dataServiceConfigDto.setServiceType("GRAPHQL");
        dataServiceConfigDto.setEndpoint("https://api.example.com/graphql");
        dataServiceConfigDto.setQuery("query getCustomer($id: ID!) { customer(id: $id) { id name email } }");

        createRequest = new CreateEntityTypeRequest();
        createRequest.setTypeName("Customer");
        createRequest.setDescription("Customer entity type");
        createRequest.setDataServiceConfig(dataServiceConfigDto);
        createRequest.setFieldMappings(Map.of("id", "customer.id", "name", "customer.name"));

        updateRequest = new UpdateEntityTypeRequest();
        updateRequest.setDescription("Updated customer entity type");
        updateRequest.setDataServiceConfig(dataServiceConfigDto);

        entityTypeResponse = new EntityTypeResponse();
        entityTypeResponse.setId(1L);
        entityTypeResponse.setTypeName("Customer");
        entityTypeResponse.setDescription("Customer entity type");
        entityTypeResponse.setDataServiceConfig(dataServiceConfigDto);
        entityTypeResponse.setFieldMappings(Map.of("id", "customer.id", "name", "customer.name"));
        entityTypeResponse.setCreatedAt(LocalDateTime.now());
        entityTypeResponse.setUpdatedAt(LocalDateTime.now());
        entityTypeResponse.setIsActive(true);
    }

    @Test
    void createEntityType_Success() throws Exception {
        // Given
        when(entityTypeService.createEntityType(any(CreateEntityTypeRequest.class)))
            .thenReturn(entityTypeResponse);

        // When & Then
        mockMvc.perform(post("/api/entity-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.typeName").value("Customer"))
                .andExpect(jsonPath("$.description").value("Customer entity type"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(entityTypeService).createEntityType(any(CreateEntityTypeRequest.class));
    }

    @Test
    void createEntityType_DuplicateName_ReturnsBadRequest() throws Exception {
        // Given
        when(entityTypeService.createEntityType(any(CreateEntityTypeRequest.class)))
            .thenThrow(new DataIntegrityViolationException("Entity type with name 'Customer' already exists"));

        // When & Then
        mockMvc.perform(post("/api/entity-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict());

        verify(entityTypeService).createEntityType(any(CreateEntityTypeRequest.class));
    }

    @Test
    void createEntityType_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        createRequest.setTypeName(""); // Invalid empty name

        // When & Then
        mockMvc.perform(post("/api/entity-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(entityTypeService, never()).createEntityType(any());
    }

    @Test
    void getAllEntityTypes_Success() throws Exception {
        // Given
        List<EntityTypeResponse> responses = List.of(entityTypeResponse);
        when(entityTypeService.getAllEntityTypes()).thenReturn(responses);

        // When & Then
        mockMvc.perform(get("/api/entity-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].typeName").value("Customer"));

        verify(entityTypeService).getAllEntityTypes();
    }

    @Test
    void getAllEntityTypes_WithInheritance_Success() throws Exception {
        // Given
        List<EntityTypeResponse> responses = List.of(entityTypeResponse);
        when(entityTypeService.getEntityTypesWithInheritance()).thenReturn(responses);

        // When & Then
        mockMvc.perform(get("/api/entity-types")
                .param("includeInheritance", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].typeName").value("Customer"));

        verify(entityTypeService).getEntityTypesWithInheritance();
        verify(entityTypeService, never()).getAllEntityTypes();
    }

    @Test
    void getEntityType_Success() throws Exception {
        // Given
        when(entityTypeService.getEntityType(1L)).thenReturn(entityTypeResponse);

        // When & Then
        mockMvc.perform(get("/api/entity-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.typeName").value("Customer"))
                .andExpect(jsonPath("$.description").value("Customer entity type"));

        verify(entityTypeService).getEntityType(1L);
    }

    @Test
    void getEntityType_NotFound_ReturnsNotFound() throws Exception {
        // Given
        when(entityTypeService.getEntityType(1L))
            .thenThrow(new RuleNotFoundException("Entity type not found with ID: 1"));

        // When & Then
        mockMvc.perform(get("/api/entity-types/1"))
                .andExpect(status().isNotFound());

        verify(entityTypeService).getEntityType(1L);
    }

    @Test
    void updateEntityType_Success() throws Exception {
        // Given
        when(entityTypeService.updateEntityType(eq(1L), any(UpdateEntityTypeRequest.class)))
            .thenReturn(entityTypeResponse);

        // When & Then
        mockMvc.perform(put("/api/entity-types/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.typeName").value("Customer"));

        verify(entityTypeService).updateEntityType(eq(1L), any(UpdateEntityTypeRequest.class));
    }

    @Test
    void updateEntityType_NotFound_ReturnsNotFound() throws Exception {
        // Given
        when(entityTypeService.updateEntityType(eq(1L), any(UpdateEntityTypeRequest.class)))
            .thenThrow(new RuleNotFoundException("Entity type not found with ID: 1"));

        // When & Then
        mockMvc.perform(put("/api/entity-types/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(entityTypeService).updateEntityType(eq(1L), any(UpdateEntityTypeRequest.class));
    }

    @Test
    void deleteEntityType_Success() throws Exception {
        // Given
        doNothing().when(entityTypeService).deleteEntityType(1L);

        // When & Then
        mockMvc.perform(delete("/api/entity-types/1"))
                .andExpect(status().isNoContent());

        verify(entityTypeService).deleteEntityType(1L);
    }

    @Test
    void deleteEntityType_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new RuleNotFoundException("Entity type not found with ID: 1"))
            .when(entityTypeService).deleteEntityType(1L);

        // When & Then
        mockMvc.perform(delete("/api/entity-types/1"))
                .andExpect(status().isNotFound());

        verify(entityTypeService).deleteEntityType(1L);
    }

    @Test
    void validateEntityTypeConfiguration_Success() throws Exception {
        // Given
        when(entityTypeService.getEntityType(1L)).thenReturn(entityTypeResponse);
        doNothing().when(entityTypeService).validateEntityTypeConfiguration(any(CreateEntityTypeRequest.class));

        // When & Then
        mockMvc.perform(post("/api/entity-types/1/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Entity type configuration is valid"));

        verify(entityTypeService).getEntityType(1L);
        verify(entityTypeService).validateEntityTypeConfiguration(any(CreateEntityTypeRequest.class));
    }

    @Test
    void validateEntityTypeConfiguration_Invalid_ReturnsBadRequest() throws Exception {
        // Given
        when(entityTypeService.getEntityType(1L)).thenReturn(entityTypeResponse);
        doThrow(new RuntimeException("Invalid configuration"))
            .when(entityTypeService).validateEntityTypeConfiguration(any(CreateEntityTypeRequest.class));

        // When & Then
        mockMvc.perform(post("/api/entity-types/1/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Invalid configuration"));

        verify(entityTypeService).getEntityType(1L);
        verify(entityTypeService).validateEntityTypeConfiguration(any(CreateEntityTypeRequest.class));
    }

    @Test
    void retrieveEntityData_Success() throws Exception {
        // Given
        Map<String, Object> entityData = Map.of("id", "123", "name", "John Doe", "email", "john@example.com");
        when(entityTypeService.retrieveEntityData("Customer", "123")).thenReturn(entityData);

        // When & Then
        mockMvc.perform(get("/api/entity-types/Customer/data/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(entityTypeService).retrieveEntityData("Customer", "123");
    }

    @Test
    void retrieveEntityData_EntityTypeNotFound_ReturnsNotFound() throws Exception {
        // Given
        when(entityTypeService.retrieveEntityData("NonExistent", "123"))
            .thenThrow(new RuleNotFoundException("Entity type not found: NonExistent"));

        // When & Then
        mockMvc.perform(get("/api/entity-types/NonExistent/data/123"))
                .andExpect(status().isNotFound());

        verify(entityTypeService).retrieveEntityData("NonExistent", "123");
    }
}