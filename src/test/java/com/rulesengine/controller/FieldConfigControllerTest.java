package com.rulesengine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.CreateFieldConfigRequest;
import com.rulesengine.dto.FieldConfigResponse;
import com.rulesengine.dto.UpdateFieldConfigRequest;
import com.rulesengine.service.FieldConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FieldConfigControllerTest {

    @Mock
    private FieldConfigService fieldConfigService;

    @InjectMocks
    private FieldConfigController fieldConfigController;

    private CreateFieldConfigRequest createRequest;
    private UpdateFieldConfigRequest updateRequest;
    private FieldConfigResponse fieldConfigResponse;

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

        fieldConfigResponse = new FieldConfigResponse();
        fieldConfigResponse.setId(1L);
        fieldConfigResponse.setFieldName("testField");
        fieldConfigResponse.setFieldType("STRING");
        fieldConfigResponse.setDescription("Test field description");
        fieldConfigResponse.setIsCalculated(false);
        fieldConfigResponse.setIsRequired(false);
        fieldConfigResponse.setVersion(1);
        fieldConfigResponse.setCreatedAt(LocalDateTime.now());
        fieldConfigResponse.setCreatedBy("testUser");
    }

    @Test
    void createFieldConfig_Success() {
        // Given
        when(fieldConfigService.createFieldConfig(any(CreateFieldConfigRequest.class)))
                .thenReturn(fieldConfigResponse);

        // When
        ResponseEntity<FieldConfigResponse> response = fieldConfigController.createFieldConfig(createRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testField", response.getBody().getFieldName());
        assertEquals("STRING", response.getBody().getFieldType());
        verify(fieldConfigService).createFieldConfig(createRequest);
    }

    @Test
    void getFieldConfigs_Success() {
        // Given
        List<FieldConfigResponse> fieldConfigs = Arrays.asList(fieldConfigResponse);
        when(fieldConfigService.getFieldConfigs()).thenReturn(fieldConfigs);

        // When
        ResponseEntity<List<FieldConfigResponse>> response = fieldConfigController.getFieldConfigs();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("testField", response.getBody().get(0).getFieldName());
        verify(fieldConfigService).getFieldConfigs();
    }

    @Test
    void getFieldConfig_Success() {
        // Given
        when(fieldConfigService.getFieldConfig(1L)).thenReturn(fieldConfigResponse);

        // When
        ResponseEntity<FieldConfigResponse> response = fieldConfigController.getFieldConfig(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("testField", response.getBody().getFieldName());
        verify(fieldConfigService).getFieldConfig(1L);
    }

    @Test
    void getFieldConfigByName_Success() {
        // Given
        when(fieldConfigService.getFieldConfigByName("testField")).thenReturn(fieldConfigResponse);

        // When
        ResponseEntity<FieldConfigResponse> response = fieldConfigController.getFieldConfigByName("testField");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testField", response.getBody().getFieldName());
        verify(fieldConfigService).getFieldConfigByName("testField");
    }

    @Test
    void updateFieldConfig_Success() {
        // Given
        fieldConfigResponse.setFieldName("updatedField");
        fieldConfigResponse.setFieldType("NUMBER");
        when(fieldConfigService.updateFieldConfig(eq(1L), any(UpdateFieldConfigRequest.class)))
                .thenReturn(fieldConfigResponse);

        // When
        ResponseEntity<FieldConfigResponse> response = fieldConfigController.updateFieldConfig(1L, updateRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("updatedField", response.getBody().getFieldName());
        assertEquals("NUMBER", response.getBody().getFieldType());
        verify(fieldConfigService).updateFieldConfig(1L, updateRequest);
    }

    @Test
    void deleteFieldConfig_Success() {
        // When
        ResponseEntity<Void> response = fieldConfigController.deleteFieldConfig(1L);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(fieldConfigService).deleteFieldConfig(1L);
    }

    @Test
    void getFieldConfigsByType_Success() {
        // Given
        List<FieldConfigResponse> fieldConfigs = Arrays.asList(fieldConfigResponse);
        when(fieldConfigService.getFieldConfigsByType("STRING")).thenReturn(fieldConfigs);

        // When
        ResponseEntity<List<FieldConfigResponse>> response = fieldConfigController.getFieldConfigsByType("STRING");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("STRING", response.getBody().get(0).getFieldType());
        verify(fieldConfigService).getFieldConfigsByType("STRING");
    }

    @Test
    void getCalculatedFieldConfigs_Success() {
        // Given
        fieldConfigResponse.setIsCalculated(true);
        List<FieldConfigResponse> fieldConfigs = Arrays.asList(fieldConfigResponse);
        when(fieldConfigService.getCalculatedFieldConfigs()).thenReturn(fieldConfigs);

        // When
        ResponseEntity<List<FieldConfigResponse>> response = fieldConfigController.getCalculatedFieldConfigs();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().get(0).getIsCalculated());
        verify(fieldConfigService).getCalculatedFieldConfigs();
    }

    @Test
    void getFieldConfigsWithDataService_Success() {
        // Given
        fieldConfigResponse.setDataServiceConfigJson("{\"type\":\"REST\"}");
        List<FieldConfigResponse> fieldConfigs = Arrays.asList(fieldConfigResponse);
        when(fieldConfigService.getFieldConfigsWithDataService()).thenReturn(fieldConfigs);

        // When
        ResponseEntity<List<FieldConfigResponse>> response = fieldConfigController.getFieldConfigsWithDataService();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(fieldConfigService).getFieldConfigsWithDataService();
    }
}