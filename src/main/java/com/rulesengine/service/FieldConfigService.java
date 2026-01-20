package com.rulesengine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.BuilderFieldConfigResponse;
import com.rulesengine.dto.CreateFieldConfigRequest;
import com.rulesengine.dto.FieldConfigResponse;
import com.rulesengine.dto.UpdateFieldConfigRequest;
import com.rulesengine.entity.FieldConfigEntity;
import com.rulesengine.exception.DuplicateRuleNameException;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.exception.RuleValidationException;
import com.rulesengine.repository.FieldConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FieldConfigService {

    private static final Logger logger = LoggerFactory.getLogger(FieldConfigService.class);

    private final FieldConfigRepository fieldConfigRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public FieldConfigService(FieldConfigRepository fieldConfigRepository, ObjectMapper objectMapper) {
        this.fieldConfigRepository = fieldConfigRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new field configuration
     */
    @CacheEvict(value = "fieldConfigs", allEntries = true)
    public FieldConfigResponse createFieldConfig(CreateFieldConfigRequest request) {
        logger.info("Creating new field configuration with name: {}", request.getFieldName());

        // Validate field configuration
        validateFieldConfiguration(request);

        // Check for duplicate field name
        if (fieldConfigRepository.existsByFieldNameActive(request.getFieldName())) {
            throw new DuplicateRuleNameException("Field configuration already exists with name: " + request.getFieldName());
        }

        // Validate data service configuration if provided
        if (StringUtils.hasText(request.getDataServiceConfigJson())) {
            validateDataServiceConfiguration(request.getDataServiceConfigJson());
        }

        // Validate calculated field dependencies if it's a calculated field
        if (Boolean.TRUE.equals(request.getIsCalculated())) {
            validateCalculatedFieldConfiguration(request);
        }

        // Create field configuration entity
        FieldConfigEntity fieldConfig = new FieldConfigEntity();
        fieldConfig.setFieldName(request.getFieldName());
        fieldConfig.setFieldType(request.getFieldType());
        fieldConfig.setDescription(request.getDescription());
        fieldConfig.setDataServiceConfigJson(request.getDataServiceConfigJson());
        fieldConfig.setMapperExpression(request.getMapperExpression());
        fieldConfig.setIsCalculated(request.getIsCalculated());
        fieldConfig.setCalculatorConfigJson(request.getCalculatorConfigJson());
        fieldConfig.setDependenciesJson(request.getDependenciesJson());
        fieldConfig.setDefaultValue(request.getDefaultValue());
        fieldConfig.setIsRequired(request.getIsRequired());
        fieldConfig.setVersion(1);

        // Save field configuration
        FieldConfigEntity savedFieldConfig = fieldConfigRepository.save(fieldConfig);

        logger.info("Successfully created field configuration with ID: {}", savedFieldConfig.getId());
        return FieldConfigResponse.from(savedFieldConfig);
    }

    /**
     * Get field configurations optimized for Rule Builder UI
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "builderFieldConfigs")
    public List<BuilderFieldConfigResponse> getFieldConfigsForBuilder() {
        logger.debug("Getting field configurations optimized for Rule Builder UI");

        List<FieldConfigEntity> fieldConfigs = fieldConfigRepository.findAllActive();
        return fieldConfigs.stream()
                .map(this::convertToBuilderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all field configurations
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "fieldConfigs")
    public List<FieldConfigResponse> getFieldConfigs() {
        logger.debug("Getting all field configurations");

        List<FieldConfigEntity> fieldConfigs = fieldConfigRepository.findAllActive();
        return fieldConfigs.stream()
                .map(FieldConfigResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific field configuration by ID
     */
    @Transactional(readOnly = true)
    public FieldConfigResponse getFieldConfig(Long id) {
        logger.debug("Getting field configuration with ID: {}", id);

        FieldConfigEntity fieldConfig = fieldConfigRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException("Field configuration not found with ID: " + id));

        return FieldConfigResponse.from(fieldConfig);
    }

    /**
     * Get field configuration by field name
     */
    @Transactional(readOnly = true)
    public FieldConfigResponse getFieldConfigByName(String fieldName) {
        logger.debug("Getting field configuration with name: {}", fieldName);

        FieldConfigEntity fieldConfig = fieldConfigRepository.findByFieldNameActive(fieldName)
                .orElseThrow(() -> new RuleNotFoundException("Field configuration not found with name: " + fieldName));

        return FieldConfigResponse.from(fieldConfig);
    }

    /**
     * Update an existing field configuration
     */
    @CacheEvict(value = "fieldConfigs", allEntries = true)
    public FieldConfigResponse updateFieldConfig(Long id, UpdateFieldConfigRequest request) {
        logger.info("Updating field configuration with ID: {}", id);

        // Find existing field configuration
        FieldConfigEntity existingFieldConfig = fieldConfigRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException("Field configuration not found with ID: " + id));

        // Validate field configuration
        validateFieldConfiguration(request);

        // Check for duplicate field name (excluding current field config)
        if (fieldConfigRepository.existsByFieldNameAndIdNotActive(request.getFieldName(), id)) {
            throw new DuplicateRuleNameException("Field configuration already exists with name: " + request.getFieldName());
        }

        // Validate data service configuration if provided
        if (StringUtils.hasText(request.getDataServiceConfigJson())) {
            validateDataServiceConfiguration(request.getDataServiceConfigJson());
        }

        // Validate calculated field dependencies if it's a calculated field
        if (Boolean.TRUE.equals(request.getIsCalculated())) {
            validateCalculatedFieldConfiguration(request);
        }

        // Check if significant changes require version increment
        boolean requiresVersionIncrement = hasSignificantChanges(existingFieldConfig, request);

        // Update field configuration properties
        existingFieldConfig.setFieldName(request.getFieldName());
        existingFieldConfig.setFieldType(request.getFieldType());
        existingFieldConfig.setDescription(request.getDescription());
        existingFieldConfig.setDataServiceConfigJson(request.getDataServiceConfigJson());
        existingFieldConfig.setMapperExpression(request.getMapperExpression());
        existingFieldConfig.setIsCalculated(request.getIsCalculated());
        existingFieldConfig.setCalculatorConfigJson(request.getCalculatorConfigJson());
        existingFieldConfig.setDependenciesJson(request.getDependenciesJson());
        existingFieldConfig.setDefaultValue(request.getDefaultValue());
        existingFieldConfig.setIsRequired(request.getIsRequired());

        // Increment version if significant changes
        if (requiresVersionIncrement) {
            existingFieldConfig.incrementVersion();
        }

        // Save updated field configuration
        FieldConfigEntity updatedFieldConfig = fieldConfigRepository.save(existingFieldConfig);

        logger.info("Successfully updated field configuration with ID: {}", updatedFieldConfig.getId());
        return FieldConfigResponse.from(updatedFieldConfig);
    }

    /**
     * Delete a field configuration (soft delete)
     */
    @CacheEvict(value = "fieldConfigs", allEntries = true)
    public void deleteFieldConfig(Long id) {
        logger.info("Deleting field configuration with ID: {}", id);

        FieldConfigEntity fieldConfig = fieldConfigRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException("Field configuration not found with ID: " + id));

        // Perform soft delete
        String currentUser = getCurrentUser();
        fieldConfig.softDelete(currentUser);
        fieldConfigRepository.save(fieldConfig);

        logger.info("Successfully deleted field configuration with ID: {}", id);
    }

    /**
     * Get field configurations by type
     */
    @Transactional(readOnly = true)
    public List<FieldConfigResponse> getFieldConfigsByType(String fieldType) {
        logger.debug("Getting field configurations with type: {}", fieldType);

        List<FieldConfigEntity> fieldConfigs = fieldConfigRepository.findByFieldTypeActive(fieldType);
        return fieldConfigs.stream()
                .map(FieldConfigResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get calculated field configurations
     */
    @Transactional(readOnly = true)
    public List<FieldConfigResponse> getCalculatedFieldConfigs() {
        logger.debug("Getting calculated field configurations");

        List<FieldConfigEntity> fieldConfigs = fieldConfigRepository.findCalculatedFieldsActive();
        return fieldConfigs.stream()
                .map(FieldConfigResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get field configurations with data services
     */
    @Transactional(readOnly = true)
    public List<FieldConfigResponse> getFieldConfigsWithDataService() {
        logger.debug("Getting field configurations with data services");

        List<FieldConfigEntity> fieldConfigs = fieldConfigRepository.findFieldsWithDataServiceActive();
        return fieldConfigs.stream()
                .map(FieldConfigResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Validate field configuration
     */
    private void validateFieldConfiguration(Object request) {
        List<String> errors = new ArrayList<>();

        String fieldName = null;
        String fieldType = null;
        Boolean isCalculated = null;
        String calculatorConfigJson = null;
        String dependenciesJson = null;

        if (request instanceof CreateFieldConfigRequest) {
            CreateFieldConfigRequest createRequest = (CreateFieldConfigRequest) request;
            fieldName = createRequest.getFieldName();
            fieldType = createRequest.getFieldType();
            isCalculated = createRequest.getIsCalculated();
            calculatorConfigJson = createRequest.getCalculatorConfigJson();
            dependenciesJson = createRequest.getDependenciesJson();
        } else if (request instanceof UpdateFieldConfigRequest) {
            UpdateFieldConfigRequest updateRequest = (UpdateFieldConfigRequest) request;
            fieldName = updateRequest.getFieldName();
            fieldType = updateRequest.getFieldType();
            isCalculated = updateRequest.getIsCalculated();
            calculatorConfigJson = updateRequest.getCalculatorConfigJson();
            dependenciesJson = updateRequest.getDependenciesJson();
        }

        // Validate field name format
        if (StringUtils.hasText(fieldName)) {
            if (!fieldName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
                errors.add("Field name must start with a letter and contain only letters, numbers, and underscores");
            }
        }

        // Validate field type
        if (StringUtils.hasText(fieldType)) {
            List<String> validTypes = Arrays.asList("STRING", "NUMBER", "DATE", "BOOLEAN", "ARRAY", "OBJECT");
            if (!validTypes.contains(fieldType)) {
                errors.add("Field type must be one of: " + String.join(", ", validTypes));
            }
        }

        // Validate calculated field requirements
        if (Boolean.TRUE.equals(isCalculated)) {
            if (!StringUtils.hasText(calculatorConfigJson)) {
                errors.add("Calculated fields must have calculator configuration");
            }
        }

        if (!errors.isEmpty()) {
            throw new RuleValidationException(errors);
        }
    }

    /**
     * Validate data service configuration JSON
     */
    private void validateDataServiceConfiguration(String dataServiceConfigJson) {
        List<String> errors = new ArrayList<>();

        try {
            JsonNode configNode = objectMapper.readTree(dataServiceConfigJson);

            // Validate required fields
            if (!configNode.has("type")) {
                errors.add("Data service configuration must have a 'type' field");
            } else {
                String type = configNode.get("type").asText();
                if (!"GRAPHQL".equals(type) && !"REST".equals(type)) {
                    errors.add("Data service type must be 'GRAPHQL' or 'REST'");
                }
            }

            if (!configNode.has("endpoint")) {
                errors.add("Data service configuration must have an 'endpoint' field");
            } else {
                String endpoint = configNode.get("endpoint").asText();
                if (!StringUtils.hasText(endpoint)) {
                    errors.add("Data service endpoint cannot be empty");
                }
            }

            // Validate GraphQL specific fields
            if (configNode.has("type") && "GRAPHQL".equals(configNode.get("type").asText())) {
                if (!configNode.has("query")) {
                    errors.add("GraphQL data service must have a 'query' field");
                }
            }

            // Validate authentication configuration if present
            if (configNode.has("auth")) {
                JsonNode authNode = configNode.get("auth");
                if (!authNode.has("type")) {
                    errors.add("Authentication configuration must have a 'type' field");
                } else {
                    String authType = authNode.get("type").asText();
                    List<String> validAuthTypes = Arrays.asList("NONE", "API_KEY", "BEARER_TOKEN", "BASIC", "OAUTH2");
                    if (!validAuthTypes.contains(authType)) {
                        errors.add("Authentication type must be one of: " + String.join(", ", validAuthTypes));
                    }
                }
            }

        } catch (Exception e) {
            errors.add("Invalid JSON format in data service configuration: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new RuleValidationException(errors);
        }
    }

    /**
     * Validate calculated field configuration
     */
    private void validateCalculatedFieldConfiguration(Object request) {
        List<String> errors = new ArrayList<>();

        String calculatorConfigJson = null;
        String dependenciesJson = null;

        if (request instanceof CreateFieldConfigRequest) {
            CreateFieldConfigRequest createRequest = (CreateFieldConfigRequest) request;
            calculatorConfigJson = createRequest.getCalculatorConfigJson();
            dependenciesJson = createRequest.getDependenciesJson();
        } else if (request instanceof UpdateFieldConfigRequest) {
            UpdateFieldConfigRequest updateRequest = (UpdateFieldConfigRequest) request;
            calculatorConfigJson = updateRequest.getCalculatorConfigJson();
            dependenciesJson = updateRequest.getDependenciesJson();
        }

        // Validate calculator configuration
        if (StringUtils.hasText(calculatorConfigJson)) {
            try {
                JsonNode calculatorNode = objectMapper.readTree(calculatorConfigJson);

                if (!calculatorNode.has("type")) {
                    errors.add("Calculator configuration must have a 'type' field");
                } else {
                    String calculatorType = calculatorNode.get("type").asText();
                    List<String> validTypes = Arrays.asList("EXPRESSION", "CUSTOM_CLASS", "BUILT_IN");
                    if (!validTypes.contains(calculatorType)) {
                        errors.add("Calculator type must be one of: " + String.join(", ", validTypes));
                    }

                    // Validate type-specific requirements
                    if ("EXPRESSION".equals(calculatorType) && !calculatorNode.has("expression")) {
                        errors.add("Expression calculator must have an 'expression' field");
                    }
                    if ("CUSTOM_CLASS".equals(calculatorType) && !calculatorNode.has("className")) {
                        errors.add("Custom class calculator must have a 'className' field");
                    }
                    if ("BUILT_IN".equals(calculatorType) && !calculatorNode.has("function")) {
                        errors.add("Built-in calculator must have a 'function' field");
                    }
                }

            } catch (Exception e) {
                errors.add("Invalid JSON format in calculator configuration: " + e.getMessage());
            }
        }

        // Validate dependencies
        if (StringUtils.hasText(dependenciesJson)) {
            try {
                JsonNode dependenciesNode = objectMapper.readTree(dependenciesJson);
                if (!dependenciesNode.isArray()) {
                    errors.add("Dependencies must be a JSON array");
                } else {
                    // Check for circular dependencies (basic validation)
                    validateDependencyGraph(dependenciesNode);
                }
            } catch (Exception e) {
                errors.add("Invalid JSON format in dependencies: " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new RuleValidationException(errors);
        }
    }

    /**
     * Validate dependency graph for circular dependencies
     */
    private void validateDependencyGraph(JsonNode dependenciesNode) {
        // This is a basic validation - a more comprehensive implementation
        // would build a full dependency graph and detect cycles
        Set<String> dependencies = new HashSet<>();
        for (JsonNode dependency : dependenciesNode) {
            String depName = dependency.asText();
            if (dependencies.contains(depName)) {
                throw new RuleValidationException("Duplicate dependency found: " + depName);
            }
            dependencies.add(depName);
        }
    }

    /**
     * Check if changes require version increment
     */
    private boolean hasSignificantChanges(FieldConfigEntity existing, UpdateFieldConfigRequest request) {
        return !Objects.equals(existing.getFieldType(), request.getFieldType()) ||
               !Objects.equals(existing.getDataServiceConfigJson(), request.getDataServiceConfigJson()) ||
               !Objects.equals(existing.getMapperExpression(), request.getMapperExpression()) ||
               !Objects.equals(existing.getIsCalculated(), request.getIsCalculated()) ||
               !Objects.equals(existing.getCalculatorConfigJson(), request.getCalculatorConfigJson()) ||
               !Objects.equals(existing.getDependenciesJson(), request.getDependenciesJson()) ||
               !Objects.equals(existing.getIsRequired(), request.getIsRequired());
    }

    /**
     * Get current authenticated user
     */
    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    /**
     * Convert FieldConfigEntity to BuilderFieldConfigResponse with operators and validation rules
     */
    private BuilderFieldConfigResponse convertToBuilderResponse(FieldConfigEntity fieldConfig) {
        // Get available operators for field type
        List<BuilderFieldConfigResponse.OperatorInfo> operators = getOperatorsForFieldType(fieldConfig.getFieldType());

        // Build validation rules
        BuilderFieldConfigResponse.ValidationRules validationRules = buildValidationRules(fieldConfig);

        // Parse dependencies
        List<String> dependencies = parseDependencies(fieldConfig.getDependenciesJson());

        // Check if field has searchable values (has data service or is calculated)
        boolean hasSearchableValues = StringUtils.hasText(fieldConfig.getDataServiceConfigJson()) || 
                                     Boolean.TRUE.equals(fieldConfig.getIsCalculated());

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hasDataService", StringUtils.hasText(fieldConfig.getDataServiceConfigJson()));
        metadata.put("hasMapper", StringUtils.hasText(fieldConfig.getMapperExpression()));
        if (StringUtils.hasText(fieldConfig.getCalculatorConfigJson())) {
            metadata.put("calculatorType", getCalculatorType(fieldConfig.getCalculatorConfigJson()));
        }

        return new BuilderFieldConfigResponse(
            fieldConfig.getId(),
            fieldConfig.getFieldName(),
            fieldConfig.getFieldType(),
            fieldConfig.getDescription(),
            operators,
            validationRules,
            hasSearchableValues,
            fieldConfig.getIsCalculated(),
            dependencies,
            fieldConfig.getDefaultValue(),
            fieldConfig.getIsRequired(),
            fieldConfig.getVersion(),
            fieldConfig.getCreatedAt(),
            metadata
        );
    }

    /**
     * Get available operators for a field type
     */
    private List<BuilderFieldConfigResponse.OperatorInfo> getOperatorsForFieldType(String fieldType) {
        List<BuilderFieldConfigResponse.OperatorInfo> operators = new ArrayList<>();

        switch (fieldType.toUpperCase()) {
            case "STRING":
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("EQUALS", "equals", "Exact match", true, "STRING"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_EQUALS", "not equals", "Not equal to", true, "STRING"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("CONTAINS", "contains", "Contains substring", true, "STRING"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_CONTAINS", "does not contain", "Does not contain substring", true, "STRING"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("STARTS_WITH", "starts with", "Starts with prefix", true, "STRING"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("ENDS_WITH", "ends with", "Ends with suffix", true, "STRING"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_EMPTY", "is empty", "Is empty or null", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NOT_EMPTY", "is not empty", "Is not empty", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IN", "in", "Is one of the values", true, "ARRAY"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_IN", "not in", "Is not one of the values", true, "ARRAY"));
                break;

            case "NUMBER":
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("EQUALS", "equals", "Equal to", true, "NUMBER"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_EQUALS", "not equals", "Not equal to", true, "NUMBER"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("GREATER_THAN", "greater than", "Greater than", true, "NUMBER"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("GREATER_THAN_OR_EQUAL", "greater than or equal", "Greater than or equal to", true, "NUMBER"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("LESS_THAN", "less than", "Less than", true, "NUMBER"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("LESS_THAN_OR_EQUAL", "less than or equal", "Less than or equal to", true, "NUMBER"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("BETWEEN", "between", "Between two values", true, "RANGE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_BETWEEN", "not between", "Not between two values", true, "RANGE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NULL", "is null", "Is null", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NOT_NULL", "is not null", "Is not null", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IN", "in", "Is one of the values", true, "ARRAY"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_IN", "not in", "Is not one of the values", true, "ARRAY"));
                break;

            case "DATE":
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("EQUALS", "equals", "Equal to date", true, "DATE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_EQUALS", "not equals", "Not equal to date", true, "DATE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("AFTER", "after", "After date", true, "DATE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("AFTER_OR_EQUAL", "after or equal", "After or equal to date", true, "DATE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("BEFORE", "before", "Before date", true, "DATE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("BEFORE_OR_EQUAL", "before or equal", "Before or equal to date", true, "DATE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("BETWEEN", "between", "Between two dates", true, "DATE_RANGE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_BETWEEN", "not between", "Not between two dates", true, "DATE_RANGE"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NULL", "is null", "Is null", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NOT_NULL", "is not null", "Is not null", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IN", "in", "Is one of the dates", true, "ARRAY"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_IN", "not in", "Is not one of the dates", true, "ARRAY"));
                break;

            case "BOOLEAN":
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("EQUALS", "equals", "Equal to", true, "BOOLEAN"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_EQUALS", "not equals", "Not equal to", true, "BOOLEAN"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_TRUE", "is true", "Is true", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_FALSE", "is false", "Is false", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NULL", "is null", "Is null", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NOT_NULL", "is not null", "Is not null", false, null));
                break;

            case "ARRAY":
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("CONTAINS", "contains", "Contains element", true, "ANY"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_CONTAINS", "does not contain", "Does not contain element", true, "ANY"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("CONTAINS_ALL", "contains all", "Contains all elements", true, "ARRAY"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("CONTAINS_ANY", "contains any", "Contains any element", true, "ARRAY"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("SIZE_EQUALS", "size equals", "Array size equals", true, "NUMBER"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("SIZE_GREATER_THAN", "size greater than", "Array size greater than", true, "NUMBER"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("SIZE_LESS_THAN", "size less than", "Array size less than", true, "NUMBER"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_EMPTY", "is empty", "Is empty array", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NOT_EMPTY", "is not empty", "Is not empty array", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NULL", "is null", "Is null", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NOT_NULL", "is not null", "Is not null", false, null));
                break;

            case "OBJECT":
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("HAS_PROPERTY", "has property", "Has property", true, "STRING"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("PROPERTY_EQUALS", "property equals", "Property equals value", true, "OBJECT"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NULL", "is null", "Is null", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NOT_NULL", "is not null", "Is not null", false, null));
                break;

            default:
                // Generic operators for unknown types
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("EQUALS", "equals", "Equal to", true, "ANY"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("NOT_EQUALS", "not equals", "Not equal to", true, "ANY"));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NULL", "is null", "Is null", false, null));
                operators.add(new BuilderFieldConfigResponse.OperatorInfo("IS_NOT_NULL", "is not null", "Is not null", false, null));
        }

        return operators;
    }

    /**
     * Build validation rules for a field configuration
     */
    private BuilderFieldConfigResponse.ValidationRules buildValidationRules(FieldConfigEntity fieldConfig) {
        BuilderFieldConfigResponse.ValidationRules validationRules = new BuilderFieldConfigResponse.ValidationRules();

        // Set basic validation based on field type
        switch (fieldConfig.getFieldType().toUpperCase()) {
            case "STRING":
                validationRules.setMinLength(0);
                validationRules.setMaxLength(1000);
                break;
            case "NUMBER":
                validationRules.setMinValue(Double.NEGATIVE_INFINITY);
                validationRules.setMaxValue(Double.POSITIVE_INFINITY);
                break;
            case "DATE":
                validationRules.setPattern("^\\d{4}-\\d{2}-\\d{2}$");
                validationRules.setValidationMessage("Date must be in YYYY-MM-DD format");
                break;
        }

        // Add custom validation message if field is required
        if (Boolean.TRUE.equals(fieldConfig.getIsRequired())) {
            validationRules.setValidationMessage("This field is required");
        }

        return validationRules;
    }

    /**
     * Parse dependencies JSON to list of field names
     */
    private List<String> parseDependencies(String dependenciesJson) {
        if (!StringUtils.hasText(dependenciesJson)) {
            return new ArrayList<>();
        }

        try {
            JsonNode dependenciesNode = objectMapper.readTree(dependenciesJson);
            List<String> dependencies = new ArrayList<>();
            
            if (dependenciesNode.isArray()) {
                for (JsonNode dependency : dependenciesNode) {
                    dependencies.add(dependency.asText());
                }
            }
            
            return dependencies;
        } catch (Exception e) {
            logger.warn("Failed to parse dependencies JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get calculator type from calculator configuration JSON
     */
    private String getCalculatorType(String calculatorConfigJson) {
        try {
            JsonNode calculatorNode = objectMapper.readTree(calculatorConfigJson);
            return calculatorNode.has("type") ? calculatorNode.get("type").asText() : "UNKNOWN";
        } catch (Exception e) {
            logger.warn("Failed to parse calculator configuration JSON: {}", e.getMessage());
            return "UNKNOWN";
        }
    }
}