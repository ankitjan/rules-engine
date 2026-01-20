package com.rulesengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.client.config.DataServiceConfig;
import com.rulesengine.client.config.GraphQLServiceConfig;
import com.rulesengine.client.config.RestServiceConfig;
import com.rulesengine.dto.*;
import com.rulesengine.entity.EntityTypeEntity;
import com.rulesengine.exception.DataServiceException;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.repository.EntityTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class EntityTypeService {

    private static final Logger logger = LoggerFactory.getLogger(EntityTypeService.class);

    private final EntityTypeRepository entityTypeRepository;
    private final DataServiceClient dataServiceClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public EntityTypeService(EntityTypeRepository entityTypeRepository, 
                           DataServiceClient dataServiceClient,
                           ObjectMapper objectMapper) {
        this.entityTypeRepository = entityTypeRepository;
        this.dataServiceClient = dataServiceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new entity type with validation
     */
    public EntityTypeResponse createEntityType(CreateEntityTypeRequest request) {
        logger.info("Creating entity type: {}", request.getTypeName());

        // Validate unique type name
        if (entityTypeRepository.existsByTypeNameActive(request.getTypeName())) {
            throw new DataIntegrityViolationException("Entity type with name '" + request.getTypeName() + "' already exists");
        }

        // Validate parent type exists if specified
        if (request.getParentTypeName() != null) {
            validateParentTypeExists(request.getParentTypeName());
        }

        // Validate data service configuration
        DataServiceConfig dataServiceConfig = convertToDataServiceConfig(request.getDataServiceConfig());
        validateDataServiceConfiguration(dataServiceConfig);

        // Create entity
        EntityTypeEntity entity = new EntityTypeEntity();
        entity.setTypeName(request.getTypeName());
        entity.setDescription(request.getDescription());
        
        try {
            entity.setDataServiceConfigJson(objectMapper.writeValueAsString(request.getDataServiceConfig()));
            if (request.getFieldMappings() != null) {
                entity.setFieldMappingsJson(objectMapper.writeValueAsString(request.getFieldMappings()));
            }
            if (request.getMetadata() != null) {
                entity.setMetadataJson(objectMapper.writeValueAsString(request.getMetadata()));
            }
            entity.setParentTypeName(request.getParentTypeName());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON in request data", e);
        }

        EntityTypeEntity savedEntity = entityTypeRepository.save(entity);
        logger.info("Created entity type with ID: {}", savedEntity.getId());

        return convertToResponse(savedEntity, request.getDataServiceConfig(), request.getFieldMappings(), 
                                request.getParentTypeName(), request.getMetadata());
    }

    /**
     * Get all entity types
     */
    @Transactional(readOnly = true)
    public List<EntityTypeResponse> getAllEntityTypes() {
        logger.debug("Retrieving all entity types");
        
        List<EntityTypeEntity> entities = entityTypeRepository.findAllActive();
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get entity type by ID
     */
    @Transactional(readOnly = true)
    public EntityTypeResponse getEntityType(Long id) {
        logger.debug("Retrieving entity type with ID: {}", id);
        
        EntityTypeEntity entity = entityTypeRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException("Entity type not found with ID: " + id));
        
        return convertToResponse(entity);
    }

    /**
     * Update entity type
     */
    public EntityTypeResponse updateEntityType(Long id, UpdateEntityTypeRequest request) {
        logger.info("Updating entity type with ID: {}", id);

        EntityTypeEntity entity = entityTypeRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException("Entity type not found with ID: " + id));

        // Validate parent type exists if specified
        if (request.getParentTypeName() != null) {
            validateParentTypeExists(request.getParentTypeName());
        }

        // Update fields
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        if (request.getDataServiceConfig() != null) {
            DataServiceConfig dataServiceConfig = convertToDataServiceConfig(request.getDataServiceConfig());
            validateDataServiceConfiguration(dataServiceConfig);
            
            try {
                entity.setDataServiceConfigJson(objectMapper.writeValueAsString(request.getDataServiceConfig()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON in data service configuration", e);
            }
        }

        if (request.getFieldMappings() != null) {
            try {
                entity.setFieldMappingsJson(objectMapper.writeValueAsString(request.getFieldMappings()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON in field mappings", e);
            }
        }

        if (request.getMetadata() != null) {
            try {
                entity.setMetadataJson(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON in metadata", e);
            }
        }

        if (request.getParentTypeName() != null) {
            entity.setParentTypeName(request.getParentTypeName());
        }

        EntityTypeEntity savedEntity = entityTypeRepository.save(entity);
        logger.info("Updated entity type with ID: {}", savedEntity.getId());

        return convertToResponse(savedEntity, request.getDataServiceConfig(), request.getFieldMappings(),
                                request.getParentTypeName(), request.getMetadata());
    }

    /**
     * Delete entity type (soft delete)
     */
    public void deleteEntityType(Long id) {
        logger.info("Deleting entity type with ID: {}", id);

        EntityTypeEntity entity = entityTypeRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException("Entity type not found with ID: " + id));

        entity.setIsDeleted(true);
        entityTypeRepository.save(entity);
        
        logger.info("Deleted entity type with ID: {}", id);
    }

    /**
     * Retrieve entity data from configured data service
     */
    @Transactional(readOnly = true)
    public Map<String, Object> retrieveEntityData(String typeName, String entityId) {
        logger.debug("Retrieving entity data for type: {} and ID: {}", typeName, entityId);

        EntityTypeEntity entityType = entityTypeRepository.findByTypeNameActive(typeName)
                .orElseThrow(() -> new RuleNotFoundException("Entity type not found: " + typeName));

        try {
            DataServiceConfigDto configDto = objectMapper.readValue(entityType.getDataServiceConfigJson(), DataServiceConfigDto.class);
            DataServiceConfig config = convertToDataServiceConfig(configDto);
            
            // Create parameters map with entity ID
            Map<String, Object> parameters = Map.of(
                "entityId", entityId,
                "id", entityId
            );

            // Retrieve data from configured service
            Object rawData = dataServiceClient.executeRequest(config, parameters);
            
            // Convert to Map if needed
            Map<String, Object> entityData;
            if (rawData instanceof Map) {
                entityData = (Map<String, Object>) rawData;
            } else {
                // If it's not a Map, wrap it in a Map
                entityData = Map.of("data", rawData);
            }
            
            // Apply field mappings if configured
            if (entityType.hasFieldMappings()) {
                Map<String, String> fieldMappings = objectMapper.readValue(entityType.getFieldMappingsJson(), Map.class);
                entityData = applyFieldMappings(entityData, fieldMappings);
            }

            logger.debug("Retrieved entity data for type: {} and ID: {}", typeName, entityId);
            return entityData;
            
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSON in entity type configuration", e);
        } catch (Exception e) {
            throw new DataServiceException("Failed to retrieve entity data for type: " + typeName, e);
        }
    }

    /**
     * Validate entity type configuration including data service connectivity
     */
    public void validateEntityTypeConfiguration(CreateEntityTypeRequest request) {
        logger.debug("Validating entity type configuration for: {}", request.getTypeName());

        // Validate data service configuration
        DataServiceConfig dataServiceConfig = convertToDataServiceConfig(request.getDataServiceConfig());
        validateDataServiceConfiguration(dataServiceConfig);

        // Validate parent type if specified
        if (request.getParentTypeName() != null) {
            validateParentTypeExists(request.getParentTypeName());
        }

        // Validate field mappings format
        if (request.getFieldMappings() != null) {
            validateFieldMappings(request.getFieldMappings());
        }

        logger.debug("Entity type configuration validation passed for: {}", request.getTypeName());
    }

    /**
     * Get entity types with inheritance support
     */
    @Transactional(readOnly = true)
    public List<EntityTypeResponse> getEntityTypesWithInheritance() {
        logger.debug("Retrieving entity types with inheritance information");
        
        List<EntityTypeEntity> entities = entityTypeRepository.findAllActive();
        return entities.stream()
                .map(this::convertToResponseWithInheritance)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private void validateParentTypeExists(String parentTypeName) {
        if (!entityTypeRepository.existsByTypeNameActive(parentTypeName)) {
            throw new IllegalArgumentException("Parent entity type not found: " + parentTypeName);
        }
    }

    private void validateDataServiceConfiguration(DataServiceConfig config) {
        try {
            dataServiceClient.validateConnection(config);
        } catch (Exception e) {
            throw new DataServiceException("Data service configuration validation failed: " + e.getMessage(), e);
        }
    }

    private void validateFieldMappings(Map<String, String> fieldMappings) {
        for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new IllegalArgumentException("Field mapping key cannot be empty");
            }
            if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                throw new IllegalArgumentException("Field mapping value cannot be empty for key: " + entry.getKey());
            }
        }
    }

    private DataServiceConfig convertToDataServiceConfig(DataServiceConfigDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Data service configuration is required");
        }

        DataServiceConfig config;
        
        switch (dto.getServiceType()) {
            case "GRAPHQL":
                GraphQLServiceConfig graphQLConfig = new GraphQLServiceConfig();
                graphQLConfig.setEndpoint(dto.getEndpoint());
                graphQLConfig.setQuery(dto.getQuery());
                graphQLConfig.setTimeoutMs(dto.getTimeoutMs() != null ? dto.getTimeoutMs() : 5000);
                graphQLConfig.setMaxRetries(dto.getRetryAttempts() != null ? dto.getRetryAttempts() : 3);
                
                // Convert authentication if present
                if (dto.getAuthentication() != null) {
                    graphQLConfig.setAuthConfig(convertToAuthConfig(dto.getAuthentication()));
                }
                
                config = graphQLConfig;
                break;
                
            case "REST":
                RestServiceConfig restConfig = new RestServiceConfig();
                restConfig.setEndpoint(dto.getEndpoint());
                restConfig.setHeaders(dto.getHeaders());
                restConfig.setTimeoutMs(dto.getTimeoutMs() != null ? dto.getTimeoutMs() : 5000);
                restConfig.setMaxRetries(dto.getRetryAttempts() != null ? dto.getRetryAttempts() : 3);
                
                // Convert authentication if present
                if (dto.getAuthentication() != null) {
                    restConfig.setAuthConfig(convertToAuthConfig(dto.getAuthentication()));
                }
                
                config = restConfig;
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported service type: " + dto.getServiceType());
        }

        return config;
    }

    private com.rulesengine.client.config.AuthConfig convertToAuthConfig(AuthConfigDto dto) {
        // This would need to be implemented based on the actual AuthConfig class structure
        // For now, returning null as a placeholder
        return null;
    }

    private Map<String, Object> applyFieldMappings(Map<String, Object> entityData, Map<String, String> fieldMappings) {
        // Apply field mappings to transform the entity data
        // This is a simplified implementation - in practice, you might want more sophisticated mapping logic
        return entityData.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> fieldMappings.getOrDefault(entry.getKey(), entry.getKey()),
                    Map.Entry::getValue
                ));
    }

    private EntityTypeResponse convertToResponse(EntityTypeEntity entity) {
        EntityTypeResponse response = new EntityTypeResponse();
        response.setId(entity.getId());
        response.setTypeName(entity.getTypeName());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedBy(entity.getUpdatedBy());
        response.setIsActive(!entity.getIsDeleted());

        // Parse JSON fields
        try {
            if (entity.getDataServiceConfigJson() != null) {
                DataServiceConfigDto configDto = objectMapper.readValue(entity.getDataServiceConfigJson(), DataServiceConfigDto.class);
                response.setDataServiceConfig(configDto);
            }
            if (entity.getFieldMappingsJson() != null) {
                Map<String, String> fieldMappings = objectMapper.readValue(entity.getFieldMappingsJson(), Map.class);
                response.setFieldMappings(fieldMappings);
            }
            if (entity.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(entity.getMetadataJson(), Map.class);
                response.setMetadata(metadata);
            }
            response.setParentTypeName(entity.getParentTypeName());
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse JSON fields for entity type ID: {}", entity.getId(), e);
        }

        return response;
    }

    private EntityTypeResponse convertToResponse(EntityTypeEntity entity, DataServiceConfigDto dataServiceConfig, 
                                               Map<String, String> fieldMappings, String parentTypeName, 
                                               Map<String, Object> metadata) {
        EntityTypeResponse response = convertToResponse(entity);
        
        if (dataServiceConfig != null) {
            response.setDataServiceConfig(dataServiceConfig);
        }
        if (fieldMappings != null) {
            response.setFieldMappings(fieldMappings);
        }
        response.setParentTypeName(parentTypeName);
        response.setMetadata(metadata);
        
        return response;
    }

    private EntityTypeResponse convertToResponseWithInheritance(EntityTypeEntity entity) {
        EntityTypeResponse response = convertToResponse(entity);
        
        // Add inheritance information - this would need to be implemented based on how inheritance is stored
        // For now, this is a placeholder
        
        return response;
    }
}