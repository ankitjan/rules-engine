package com.rulesengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.CreateFieldConfigRequest;
import com.rulesengine.dto.FieldConfigResponse;
import com.rulesengine.dto.UpdateFieldConfigRequest;
import com.rulesengine.entity.FieldConfigEntity;
import com.rulesengine.exception.DuplicateRuleNameException;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.exception.RuleValidationException;
import com.rulesengine.repository.FieldConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FieldConfigServiceTest {

    @Mock
    private FieldConfigRepository fieldConfigRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FieldConfigService fieldConfigService;

    private CreateFieldConfigRequest createRequest;
    private UpdateFieldConfigRequest updateRequest;
    private FieldConfigEntity fieldConfigEntity;

    @BeforeEach
    void setUp() {
        createRequest = new CreateFieldConfigRequest();
        createRequest.setFieldName("testField");
        createRequest.setFieldType("STRING");
        createRequest.setDescription("Test field description");
        createRequest.setIsCalculated(false);
        createRequest.setIsRequired(false);

        updateRequest = new UpdateFieldConfigRequest();
        updateRequest.setFieldName("updatedField");
        updateRequest.setFieldType("NUMBER");
        updateRequest.setDescription("Updated field description");
        updateRequest.setIsCalculated(false);
        updateRequest.setIsRequired(true);

        fieldConfigEntity = new FieldConfigEntity();
        fieldConfigEntity.setId(1L);
        fieldConfigEntity.setFieldName("testField");
        fieldConfigEntity.setFieldType("STRING");
        fieldConfigEntity.setDescription("Test field description");
        fieldConfigEntity.setIsCalculated(false);
        fieldConfigEntity.setIsRequired(false);
        fieldConfigEntity.setVersion(1);
    }

    @Test
    void createFieldConfig_Success() {
        // Given
        when(fieldConfigRepository.existsByFieldNameActive(createRequest.getFieldName())).thenReturn(false);
        when(fieldConfigRepository.save(any(FieldConfigEntity.class))).thenReturn(fieldConfigEntity);

        // When
        FieldConfigResponse response = fieldConfigService.createFieldConfig(createRequest);

        // Then
        assertNotNull(response);
        assertEquals("testField", response.getFieldName());
        assertEquals("STRING", response.getFieldType());
        verify(fieldConfigRepository).save(any(FieldConfigEntity.class));
    }

    @Test
    void createFieldConfig_DuplicateName_ThrowsException() {
        // Given
        when(fieldConfigRepository.existsByFieldNameActive(createRequest.getFieldName())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateRuleNameException.class, () -> {
            fieldConfigService.createFieldConfig(createRequest);
        });
        verify(fieldConfigRepository, never()).save(any(FieldConfigEntity.class));
    }

    @Test
    void createFieldConfig_InvalidFieldName_ThrowsException() {
        // Given
        createRequest.setFieldName("123invalid");

        // When & Then
        assertThrows(RuleValidationException.class, () -> {
            fieldConfigService.createFieldConfig(createRequest);
        });
    }

    @Test
    void getFieldConfigs_Success() {
        // Given
        List<FieldConfigEntity> entities = Arrays.asList(fieldConfigEntity);
        when(fieldConfigRepository.findAllActive()).thenReturn(entities);

        // When
        List<FieldConfigResponse> responses = fieldConfigService.getFieldConfigs();

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("testField", responses.get(0).getFieldName());
    }

    @Test
    void getFieldConfig_Success() {
        // Given
        when(fieldConfigRepository.findByIdActive(1L)).thenReturn(Optional.of(fieldConfigEntity));

        // When
        FieldConfigResponse response = fieldConfigService.getFieldConfig(1L);

        // Then
        assertNotNull(response);
        assertEquals("testField", response.getFieldName());
    }

    @Test
    void getFieldConfig_NotFound_ThrowsException() {
        // Given
        when(fieldConfigRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuleNotFoundException.class, () -> {
            fieldConfigService.getFieldConfig(1L);
        });
    }

    @Test
    void updateFieldConfig_Success() {
        // Given
        when(fieldConfigRepository.findByIdActive(1L)).thenReturn(Optional.of(fieldConfigEntity));
        when(fieldConfigRepository.existsByFieldNameAndIdNotActive(updateRequest.getFieldName(), 1L)).thenReturn(false);
        when(fieldConfigRepository.save(any(FieldConfigEntity.class))).thenReturn(fieldConfigEntity);

        // When
        FieldConfigResponse response = fieldConfigService.updateFieldConfig(1L, updateRequest);

        // Then
        assertNotNull(response);
        verify(fieldConfigRepository).save(any(FieldConfigEntity.class));
    }

    @Test
    void updateFieldConfig_NotFound_ThrowsException() {
        // Given
        when(fieldConfigRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuleNotFoundException.class, () -> {
            fieldConfigService.updateFieldConfig(1L, updateRequest);
        });
    }

    @Test
    void deleteFieldConfig_Success() {
        // Given
        when(fieldConfigRepository.findByIdActive(1L)).thenReturn(Optional.of(fieldConfigEntity));
        when(fieldConfigRepository.save(any(FieldConfigEntity.class))).thenReturn(fieldConfigEntity);

        // When
        fieldConfigService.deleteFieldConfig(1L);

        // Then
        verify(fieldConfigRepository).save(any(FieldConfigEntity.class));
    }

    @Test
    void deleteFieldConfig_NotFound_ThrowsException() {
        // Given
        when(fieldConfigRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuleNotFoundException.class, () -> {
            fieldConfigService.deleteFieldConfig(1L);
        });
    }

    @Test
    void getFieldConfigsByType_Success() {
        // Given
        List<FieldConfigEntity> entities = Arrays.asList(fieldConfigEntity);
        when(fieldConfigRepository.findByFieldTypeActive("STRING")).thenReturn(entities);

        // When
        List<FieldConfigResponse> responses = fieldConfigService.getFieldConfigsByType("STRING");

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("STRING", responses.get(0).getFieldType());
    }

    @Test
    void getCalculatedFieldConfigs_Success() {
        // Given
        fieldConfigEntity.setIsCalculated(true);
        List<FieldConfigEntity> entities = Arrays.asList(fieldConfigEntity);
        when(fieldConfigRepository.findCalculatedFieldsActive()).thenReturn(entities);

        // When
        List<FieldConfigResponse> responses = fieldConfigService.getCalculatedFieldConfigs();

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getIsCalculated());
    }

    @Test
    void getFieldConfigsWithDataService_Success() {
        // Given
        fieldConfigEntity.setDataServiceConfigJson("{\"type\":\"REST\",\"endpoint\":\"http://example.com\"}");
        List<FieldConfigEntity> entities = Arrays.asList(fieldConfigEntity);
        when(fieldConfigRepository.findFieldsWithDataServiceActive()).thenReturn(entities);

        // When
        List<FieldConfigResponse> responses = fieldConfigService.getFieldConfigsWithDataService();

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertNotNull(responses.get(0).getDataServiceConfigJson());
    }
}