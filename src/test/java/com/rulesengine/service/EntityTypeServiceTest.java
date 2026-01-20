package com.rulesengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.client.config.DataServiceConfig;
import com.rulesengine.dto.*;
import com.rulesengine.entity.EntityTypeEntity;
import com.rulesengine.exception.DataServiceException;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.repository.EntityTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityTypeServiceTest {

    @Mock
    private EntityTypeRepository entityTypeRepository;

    @Mock
    private DataServiceClient dataServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EntityTypeService entityTypeService;

    private CreateEntityTypeRequest createRequest;
    private UpdateEntityTypeRequest updateRequest;
    private EntityTypeEntity entityTypeEntity;
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
        createRequest.setMetadata(Map.of("version", "1.0", "category", "core"));

        updateRequest = new UpdateEntityTypeRequest();
        updateRequest.setDescription("Updated customer entity type");
        updateRequest.setDataServiceConfig(dataServiceConfigDto);

        entityTypeEntity = new EntityTypeEntity();
        entityTypeEntity.setId(1L);
        entityTypeEntity.setTypeName("Customer");
        entityTypeEntity.setDescription("Customer entity type");
        entityTypeEntity.setDataServiceConfigJson("{\"serviceType\":\"GRAPHQL\"}");
        entityTypeEntity.setFieldMappingsJson("{\"id\":\"customer.id\"}");
        entityTypeEntity.setCreatedAt(LocalDateTime.now());
        entityTypeEntity.setUpdatedAt(LocalDateTime.now());
        entityTypeEntity.setIsDeleted(false);
    }

    @Test
    void createEntityType_Success() throws Exception {
        // Given
        when(entityTypeRepository.existsByTypeNameActive("Customer")).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
        doNothing().when(dataServiceClient).validateConnection(any(DataServiceConfig.class));
        when(entityTypeRepository.save(any(EntityTypeEntity.class))).thenReturn(entityTypeEntity);

        // When
        EntityTypeResponse response = entityTypeService.createEntityType(createRequest);

        // Then
        assertNotNull(response);
        assertEquals("Customer", response.getTypeName());
        assertEquals("Customer entity type", response.getDescription());
        verify(entityTypeRepository).existsByTypeNameActive("Customer");
        verify(entityTypeRepository).save(any(EntityTypeEntity.class));
        verify(dataServiceClient).validateConnection(any(DataServiceConfig.class));
    }

    @Test
    void createEntityType_DuplicateName_ThrowsException() {
        // Given
        when(entityTypeRepository.existsByTypeNameActive("Customer")).thenReturn(true);

        // When & Then
        assertThrows(DataIntegrityViolationException.class, 
            () -> entityTypeService.createEntityType(createRequest));
        
        verify(entityTypeRepository).existsByTypeNameActive("Customer");
        verify(entityTypeRepository, never()).save(any());
    }

    @Test
    void createEntityType_InvalidParentType_ThrowsException() {
        // Given
        createRequest.setParentTypeName("NonExistentParent");
        when(entityTypeRepository.existsByTypeNameActive("Customer")).thenReturn(false);
        when(entityTypeRepository.existsByTypeNameActive("NonExistentParent")).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> entityTypeService.createEntityType(createRequest));
    }

    @Test
    void getAllEntityTypes_Success() {
        // Given
        List<EntityTypeEntity> entities = List.of(entityTypeEntity);
        when(entityTypeRepository.findAllActive()).thenReturn(entities);

        // When
        List<EntityTypeResponse> responses = entityTypeService.getAllEntityTypes();

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Customer", responses.get(0).getTypeName());
        verify(entityTypeRepository).findAllActive();
    }

    @Test
    void getEntityType_Success() {
        // Given
        when(entityTypeRepository.findByIdActive(1L)).thenReturn(Optional.of(entityTypeEntity));

        // When
        EntityTypeResponse response = entityTypeService.getEntityType(1L);

        // Then
        assertNotNull(response);
        assertEquals("Customer", response.getTypeName());
        verify(entityTypeRepository).findByIdActive(1L);
    }

    @Test
    void getEntityType_NotFound_ThrowsException() {
        // Given
        when(entityTypeRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuleNotFoundException.class, 
            () -> entityTypeService.getEntityType(1L));
        
        verify(entityTypeRepository).findByIdActive(1L);
    }

    @Test
    void updateEntityType_Success() throws Exception {
        // Given
        when(entityTypeRepository.findByIdActive(1L)).thenReturn(Optional.of(entityTypeEntity));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
        doNothing().when(dataServiceClient).validateConnection(any(DataServiceConfig.class));
        when(entityTypeRepository.save(any(EntityTypeEntity.class))).thenReturn(entityTypeEntity);

        // When
        EntityTypeResponse response = entityTypeService.updateEntityType(1L, updateRequest);

        // Then
        assertNotNull(response);
        verify(entityTypeRepository).findByIdActive(1L);
        verify(entityTypeRepository).save(any(EntityTypeEntity.class));
        verify(dataServiceClient).validateConnection(any(DataServiceConfig.class));
    }

    @Test
    void updateEntityType_NotFound_ThrowsException() {
        // Given
        when(entityTypeRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuleNotFoundException.class, 
            () -> entityTypeService.updateEntityType(1L, updateRequest));
        
        verify(entityTypeRepository).findByIdActive(1L);
        verify(entityTypeRepository, never()).save(any());
    }

    @Test
    void deleteEntityType_Success() {
        // Given
        when(entityTypeRepository.findByIdActive(1L)).thenReturn(Optional.of(entityTypeEntity));
        when(entityTypeRepository.save(any(EntityTypeEntity.class))).thenReturn(entityTypeEntity);

        // When
        entityTypeService.deleteEntityType(1L);

        // Then
        verify(entityTypeRepository).findByIdActive(1L);
        verify(entityTypeRepository).save(any(EntityTypeEntity.class));
        assertTrue(entityTypeEntity.getIsDeleted());
    }

    @Test
    void deleteEntityType_NotFound_ThrowsException() {
        // Given
        when(entityTypeRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuleNotFoundException.class, 
            () -> entityTypeService.deleteEntityType(1L));
        
        verify(entityTypeRepository).findByIdActive(1L);
        verify(entityTypeRepository, never()).save(any());
    }

    @Test
    void retrieveEntityData_Success() throws Exception {
        // Given
        String typeName = "Customer";
        String entityId = "123";
        Map<String, Object> expectedData = Map.of("id", "123", "name", "John Doe");
        
        when(entityTypeRepository.findByTypeNameActive(typeName)).thenReturn(Optional.of(entityTypeEntity));
        when(objectMapper.readValue(anyString(), eq(DataServiceConfigDto.class))).thenReturn(dataServiceConfigDto);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("id", "customer.id"));
        when(dataServiceClient.executeRequest(any(DataServiceConfig.class), any(Map.class))).thenReturn(expectedData);

        // When
        Map<String, Object> result = entityTypeService.retrieveEntityData(typeName, entityId);

        // Then
        assertNotNull(result);
        verify(entityTypeRepository).findByTypeNameActive(typeName);
        verify(dataServiceClient).executeRequest(any(DataServiceConfig.class), any(Map.class));
    }

    @Test
    void retrieveEntityData_EntityTypeNotFound_ThrowsException() {
        // Given
        String typeName = "NonExistent";
        String entityId = "123";
        
        when(entityTypeRepository.findByTypeNameActive(typeName)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuleNotFoundException.class, 
            () -> entityTypeService.retrieveEntityData(typeName, entityId));
        
        verify(entityTypeRepository).findByTypeNameActive(typeName);
        verify(dataServiceClient, never()).executeRequest(any(), any());
    }

    @Test
    void retrieveEntityData_DataServiceFailure_ThrowsException() throws Exception {
        // Given
        String typeName = "Customer";
        String entityId = "123";
        
        when(entityTypeRepository.findByTypeNameActive(typeName)).thenReturn(Optional.of(entityTypeEntity));
        when(objectMapper.readValue(anyString(), eq(DataServiceConfigDto.class))).thenReturn(dataServiceConfigDto);
        when(dataServiceClient.executeRequest(any(DataServiceConfig.class), any(Map.class)))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        assertThrows(DataServiceException.class, 
            () -> entityTypeService.retrieveEntityData(typeName, entityId));
        
        verify(entityTypeRepository).findByTypeNameActive(typeName);
        verify(dataServiceClient).executeRequest(any(DataServiceConfig.class), any(Map.class));
    }

    @Test
    void validateEntityTypeConfiguration_Success() {
        // Given
        doNothing().when(dataServiceClient).validateConnection(any(DataServiceConfig.class));

        // When & Then
        assertDoesNotThrow(() -> entityTypeService.validateEntityTypeConfiguration(createRequest));
        
        verify(dataServiceClient).validateConnection(any(DataServiceConfig.class));
    }

    @Test
    void validateEntityTypeConfiguration_InvalidDataService_ThrowsException() {
        // Given
        doThrow(new RuntimeException("Connection failed"))
            .when(dataServiceClient).validateConnection(any(DataServiceConfig.class));

        // When & Then
        assertThrows(DataServiceException.class, 
            () -> entityTypeService.validateEntityTypeConfiguration(createRequest));
        
        verify(dataServiceClient).validateConnection(any(DataServiceConfig.class));
    }

    @Test
    void getEntityTypesWithInheritance_Success() {
        // Given
        List<EntityTypeEntity> entities = List.of(entityTypeEntity);
        when(entityTypeRepository.findAllActive()).thenReturn(entities);

        // When
        List<EntityTypeResponse> responses = entityTypeService.getEntityTypesWithInheritance();

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(entityTypeRepository).findAllActive();
    }
}