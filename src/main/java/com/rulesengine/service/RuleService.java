package com.rulesengine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.CreateRuleRequest;
import com.rulesengine.dto.PagedRuleResponse;
import com.rulesengine.dto.RuleResponse;
import com.rulesengine.dto.RuleValidationRequest;
import com.rulesengine.dto.RuleValidationResult;
import com.rulesengine.dto.UpdateRuleRequest;
import com.rulesengine.entity.FieldConfigEntity;
import com.rulesengine.entity.FolderEntity;
import com.rulesengine.entity.RuleEntity;
import com.rulesengine.entity.RuleVersionEntity;
import com.rulesengine.exception.DuplicateRuleNameException;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.exception.RuleValidationException;
import com.rulesengine.repository.FieldConfigRepository;
import com.rulesengine.repository.FolderRepository;
import com.rulesengine.repository.RuleRepository;
import com.rulesengine.repository.RuleVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
@Transactional
public class RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleService.class);

    private final RuleRepository ruleRepository;
    private final FolderRepository folderRepository;
    private final RuleVersionRepository ruleVersionRepository;
    private final FieldConfigRepository fieldConfigRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public RuleService(RuleRepository ruleRepository, 
                      FolderRepository folderRepository,
                      RuleVersionRepository ruleVersionRepository,
                      FieldConfigRepository fieldConfigRepository,
                      ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.folderRepository = folderRepository;
        this.ruleVersionRepository = ruleVersionRepository;
        this.fieldConfigRepository = fieldConfigRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new rule
     */
    public RuleResponse createRule(CreateRuleRequest request) {
        logger.info("Creating new rule with name: {}", request.getName());

        // Validate rule structure
        validateRuleStructure(request.getRuleDefinitionJson());

        // Check for duplicate name
        Optional<RuleEntity> existingRule = ruleRepository.findByNameActive(request.getName());
        if (existingRule.isPresent()) {
            throw DuplicateRuleNameException.forRuleName(request.getName());
        }

        // Validate folder if specified
        FolderEntity folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findByIdActive(request.getFolderId())
                    .orElseThrow(() -> new RuleValidationException("Folder not found with ID: " + request.getFolderId()));
        }

        // Create rule entity
        RuleEntity rule = new RuleEntity();
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setRuleDefinitionJson(request.getRuleDefinitionJson());
        rule.setFolder(folder);
        rule.setVersion(1);
        rule.setIsActive(true);

        // Save rule
        RuleEntity savedRule = ruleRepository.save(rule);

        // Create initial version
        createRuleVersion(savedRule, "Initial version");

        logger.info("Successfully created rule with ID: {}", savedRule.getId());
        return RuleResponse.from(savedRule);
    }

    /**
     * Get rules with optional filtering and pagination
     */
    @Transactional(readOnly = true)
    public PagedRuleResponse getRules(String filter, Pageable pageable) {
        logger.debug("Getting rules with filter: {} and pagination: {}", filter, pageable);

        Page<RuleEntity> rulePage;

        if (StringUtils.hasText(filter)) {
            // Search by name containing filter text
            rulePage = ruleRepository.findByNameContainingIgnoreCaseActive(filter, pageable);
        } else {
            // Get all active rules
            rulePage = ruleRepository.findAllActive(pageable);
        }

        // Convert to response DTOs
        Page<RuleResponse> responsePage = rulePage.map(RuleResponse::from);

        return PagedRuleResponse.from(responsePage);
    }

    /**
     * Get a specific rule by ID
     */
    @Transactional(readOnly = true)
    public RuleResponse getRule(Long id) {
        logger.debug("Getting rule with ID: {}", id);

        RuleEntity rule = ruleRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException(id));

        return RuleResponse.from(rule);
    }

    /**
     * Update an existing rule
     */
    public RuleResponse updateRule(Long id, UpdateRuleRequest request) {
        logger.info("Updating rule with ID: {}", id);

        // Find existing rule
        RuleEntity existingRule = ruleRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException(id));

        // Validate rule structure
        validateRuleStructure(request.getRuleDefinitionJson());

        // Check for duplicate name (excluding current rule)
        Optional<RuleEntity> duplicateRule = ruleRepository.findByNameActive(request.getName());
        if (duplicateRule.isPresent() && !duplicateRule.get().getId().equals(id)) {
            throw DuplicateRuleNameException.forRuleName(request.getName());
        }

        // Validate folder if specified
        FolderEntity folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findByIdActive(request.getFolderId())
                    .orElseThrow(() -> new RuleValidationException("Folder not found with ID: " + request.getFolderId()));
        }

        // Check if rule definition changed to create new version
        boolean ruleDefinitionChanged = !existingRule.getRuleDefinitionJson().equals(request.getRuleDefinitionJson());

        // Update rule properties
        existingRule.setName(request.getName());
        existingRule.setDescription(request.getDescription());
        existingRule.setRuleDefinitionJson(request.getRuleDefinitionJson());
        existingRule.setFolder(folder);

        if (request.getIsActive() != null) {
            existingRule.setIsActive(request.getIsActive());
        }

        // Increment version if rule definition changed
        if (ruleDefinitionChanged) {
            existingRule.incrementVersion();
        }

        // Save updated rule
        RuleEntity updatedRule = ruleRepository.save(existingRule);

        // Create new version if rule definition changed
        if (ruleDefinitionChanged) {
            createRuleVersion(updatedRule, "Rule updated");
        }

        logger.info("Successfully updated rule with ID: {}", updatedRule.getId());
        return RuleResponse.from(updatedRule);
    }

    /**
     * Delete a rule (soft delete)
     */
    public void deleteRule(Long id) {
        logger.info("Deleting rule with ID: {}", id);

        RuleEntity rule = ruleRepository.findByIdActive(id)
                .orElseThrow(() -> new RuleNotFoundException(id));

        // Perform soft delete
        String currentUser = getCurrentUser();
        rule.softDelete(currentUser);
        ruleRepository.save(rule);

        logger.info("Successfully deleted rule with ID: {}", id);
    }

    /**
     * Validate rule structure
     */
    public void validateRuleStructure(String ruleDefinitionJson) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(ruleDefinitionJson)) {
            errors.add("Rule definition cannot be empty");
            throw new RuleValidationException(errors);
        }

        try {
            // Parse JSON to validate structure
            JsonNode ruleNode = objectMapper.readTree(ruleDefinitionJson);

            // Validate required fields
            if (!ruleNode.has("combinator")) {
                errors.add("Rule must have a combinator field");
            }

            if (!ruleNode.has("rules")) {
                errors.add("Rule must have a rules array");
            } else {
                JsonNode rulesArray = ruleNode.get("rules");
                if (!rulesArray.isArray()) {
                    errors.add("Rules field must be an array");
                } else if (rulesArray.size() == 0) {
                    errors.add("Rule must contain at least one condition or group");
                } else {
                    // Validate each rule in the array
                    for (int i = 0; i < rulesArray.size(); i++) {
                        validateRuleItem(rulesArray.get(i), "rules[" + i + "]", errors);
                    }
                }
            }

            // Validate combinator value
            if (ruleNode.has("combinator")) {
                String combinator = ruleNode.get("combinator").asText();
                if (!"and".equals(combinator) && !"or".equals(combinator)) {
                    errors.add("Combinator must be 'and' or 'or'");
                }
            }

        } catch (Exception e) {
            errors.add("Invalid JSON format: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            throw new RuleValidationException(errors);
        }
    }

    /**
     * Enhanced rule validation for Rule Builder UI
     */
    public RuleValidationResult validateRuleForBuilder(RuleValidationRequest request) {
        logger.debug("Performing enhanced rule validation for Rule Builder");

        List<RuleValidationResult.ValidationError> errors = new ArrayList<>();
        List<RuleValidationResult.ValidationWarning> warnings = new ArrayList<>();
        List<RuleValidationResult.ValidationSuggestion> suggestions = new ArrayList<>();

        try {
            // Parse rule definition
            JsonNode ruleNode = objectMapper.readTree(objectMapper.writeValueAsString(request.getRuleDefinition()));

            // Get available field configurations for validation
            Map<String, FieldConfigEntity> availableFields = new HashMap<>();
            if (request.getDeepValidation()) {
                List<FieldConfigEntity> fieldConfigs = fieldConfigRepository.findAllActive();
                for (FieldConfigEntity field : fieldConfigs) {
                    availableFields.put(field.getFieldName(), field);
                }
            }

            // Validate rule structure
            validateRuleStructureEnhanced(ruleNode, "", errors, warnings, suggestions, availableFields, request);

            // Check for field dependencies if requested
            if (request.getCheckFieldDependencies()) {
                validateFieldDependencies(ruleNode, errors, warnings, availableFields);
            }

        } catch (Exception e) {
            errors.add(new RuleValidationResult.ValidationError(
                "PARSE_ERROR", 
                "Failed to parse rule definition: " + e.getMessage(), 
                "root"
            ));
        }

        // Generate summary
        String summary = generateValidationSummary(errors, warnings);

        // Create metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("validationTimestamp", System.currentTimeMillis());
        metadata.put("deepValidation", request.getDeepValidation());
        metadata.put("checkOperatorCompatibility", request.getCheckOperatorCompatibility());
        metadata.put("validateValueConstraints", request.getValidateValueConstraints());

        return new RuleValidationResult(
            errors.isEmpty(),
            errors,
            warnings,
            suggestions,
            summary,
            metadata
        );
    }

    /**
     * Validate individual rule item (condition or group)
     */
    private void validateRuleItem(JsonNode ruleItem, String path, List<String> errors) {
        if (ruleItem.has("rules")) {
            // This is a group
            if (!ruleItem.has("combinator")) {
                errors.add(path + ": Group must have a combinator");
            } else {
                String combinator = ruleItem.get("combinator").asText();
                if (!"and".equals(combinator) && !"or".equals(combinator)) {
                    errors.add(path + ": Group combinator must be 'and' or 'or'");
                }
            }

            JsonNode nestedRules = ruleItem.get("rules");
            if (!nestedRules.isArray() || nestedRules.size() == 0) {
                errors.add(path + ": Group must contain at least one rule");
            } else {
                for (int i = 0; i < nestedRules.size(); i++) {
                    validateRuleItem(nestedRules.get(i), path + ".rules[" + i + "]", errors);
                }
            }
        } else {
            // This is a condition
            if (!ruleItem.has("field")) {
                errors.add(path + ": Condition must have a field");
            }
            if (!ruleItem.has("operator")) {
                errors.add(path + ": Condition must have an operator");
            }
            if (!ruleItem.has("value")) {
                errors.add(path + ": Condition must have a value");
            }

            // Validate operator
            if (ruleItem.has("operator")) {
                String operator = ruleItem.get("operator").asText();
                List<String> validOperators = List.of("=", "!=", "<", "<=", ">", ">=", 
                    "contains", "startsWith", "endsWith", "in", "notIn", "isEmpty", "isNotEmpty");
                if (!validOperators.contains(operator)) {
                    errors.add(path + ": Invalid operator '" + operator + "'");
                }
            }
        }
    }

    /**
     * Create a new version of a rule
     */
    private void createRuleVersion(RuleEntity rule, String changeDescription) {
        String currentUser = getCurrentUser();

        RuleVersionEntity version = new RuleVersionEntity();
        version.setRule(rule);
        version.setVersionNumber(rule.getVersion());
        version.setRuleDefinitionJson(rule.getRuleDefinitionJson());
        version.setChangeDescription(changeDescription);
        version.setCreatedBy(currentUser);

        ruleVersionRepository.save(version);
    }

    /**
     * Get current authenticated user
     */
    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }

    /**
     * Enhanced validation of rule structure with detailed error reporting
     */
    private void validateRuleStructureEnhanced(JsonNode ruleNode, String path, 
                                             List<RuleValidationResult.ValidationError> errors,
                                             List<RuleValidationResult.ValidationWarning> warnings,
                                             List<RuleValidationResult.ValidationSuggestion> suggestions,
                                             Map<String, FieldConfigEntity> availableFields,
                                             RuleValidationRequest request) {
        
        // Validate required fields
        if (!ruleNode.has("combinator")) {
            errors.add(new RuleValidationResult.ValidationError(
                "MISSING_COMBINATOR", 
                "Rule must have a combinator field", 
                path.isEmpty() ? "root" : path
            ));
            suggestions.add(new RuleValidationResult.ValidationSuggestion(
                "ADD_COMBINATOR",
                "Add a combinator field with value 'and' or 'or'",
                path.isEmpty() ? "root" : path,
                Map.of("combinator", "and")
            ));
        }

        if (!ruleNode.has("rules")) {
            errors.add(new RuleValidationResult.ValidationError(
                "MISSING_RULES", 
                "Rule must have a rules array", 
                path.isEmpty() ? "root" : path
            ));
        } else {
            JsonNode rulesArray = ruleNode.get("rules");
            if (!rulesArray.isArray()) {
                errors.add(new RuleValidationResult.ValidationError(
                    "INVALID_RULES_TYPE", 
                    "Rules field must be an array", 
                    path.isEmpty() ? "root.rules" : path + ".rules"
                ));
            } else if (rulesArray.size() == 0) {
                errors.add(new RuleValidationResult.ValidationError(
                    "EMPTY_RULES", 
                    "Rule must contain at least one condition or group", 
                    path.isEmpty() ? "root.rules" : path + ".rules"
                ));
            } else {
                // Validate each rule in the array
                for (int i = 0; i < rulesArray.size(); i++) {
                    String itemPath = (path.isEmpty() ? "rules[" + i + "]" : path + ".rules[" + i + "]");
                    validateRuleItemEnhanced(rulesArray.get(i), itemPath, errors, warnings, suggestions, availableFields, request);
                }
            }
        }

        // Validate combinator value
        if (ruleNode.has("combinator")) {
            String combinator = ruleNode.get("combinator").asText();
            if (!"and".equals(combinator) && !"or".equals(combinator)) {
                errors.add(new RuleValidationResult.ValidationError(
                    "INVALID_COMBINATOR", 
                    "Combinator must be 'and' or 'or'", 
                    path.isEmpty() ? "root.combinator" : path + ".combinator"
                ));
                suggestions.add(new RuleValidationResult.ValidationSuggestion(
                    "FIX_COMBINATOR",
                    "Use 'and' for all conditions to be true, or 'or' for any condition to be true",
                    path.isEmpty() ? "root.combinator" : path + ".combinator",
                    "and"
                ));
            }
        }
    }

    /**
     * Enhanced validation of individual rule items
     */
    private void validateRuleItemEnhanced(JsonNode ruleItem, String path,
                                        List<RuleValidationResult.ValidationError> errors,
                                        List<RuleValidationResult.ValidationWarning> warnings,
                                        List<RuleValidationResult.ValidationSuggestion> suggestions,
                                        Map<String, FieldConfigEntity> availableFields,
                                        RuleValidationRequest request) {
        
        if (ruleItem.has("rules")) {
            // This is a group - validate recursively
            validateRuleStructureEnhanced(ruleItem, path, errors, warnings, suggestions, availableFields, request);
        } else {
            // This is a condition
            validateConditionEnhanced(ruleItem, path, errors, warnings, suggestions, availableFields, request);
        }
    }

    /**
     * Enhanced validation of rule conditions
     */
    private void validateConditionEnhanced(JsonNode condition, String path,
                                         List<RuleValidationResult.ValidationError> errors,
                                         List<RuleValidationResult.ValidationWarning> warnings,
                                         List<RuleValidationResult.ValidationSuggestion> suggestions,
                                         Map<String, FieldConfigEntity> availableFields,
                                         RuleValidationRequest request) {
        
        // Validate required fields
        if (!condition.has("field")) {
            errors.add(new RuleValidationResult.ValidationError(
                "MISSING_FIELD", 
                "Condition must have a field", 
                path + ".field"
            ));
        } else {
            String fieldName = condition.get("field").asText();
            
            // Check if field exists in available fields
            if (request.getDeepValidation() && !availableFields.containsKey(fieldName)) {
                errors.add(new RuleValidationResult.ValidationError(
                    "UNKNOWN_FIELD", 
                    "Field '" + fieldName + "' is not defined in field configurations", 
                    path + ".field",
                    fieldName,
                    "ERROR"
                ));
                
                // Suggest similar field names
                String suggestion = findSimilarFieldName(fieldName, availableFields.keySet());
                if (suggestion != null) {
                    suggestions.add(new RuleValidationResult.ValidationSuggestion(
                        "SIMILAR_FIELD",
                        "Did you mean '" + suggestion + "'?",
                        path + ".field",
                        suggestion
                    ));
                }
            }
        }

        if (!condition.has("operator")) {
            errors.add(new RuleValidationResult.ValidationError(
                "MISSING_OPERATOR", 
                "Condition must have an operator", 
                path + ".operator"
            ));
        } else {
            String operator = condition.get("operator").asText();
            String fieldName = condition.has("field") ? condition.get("field").asText() : null;
            
            // Validate operator compatibility with field type
            if (request.getCheckOperatorCompatibility() && fieldName != null && availableFields.containsKey(fieldName)) {
                FieldConfigEntity fieldConfig = availableFields.get(fieldName);
                validateOperatorCompatibility(operator, fieldConfig, path, errors, warnings, suggestions);
            }
        }

        // Validate value field (some operators don't require values)
        String operator = condition.has("operator") ? condition.get("operator").asText() : "";
        boolean requiresValue = !List.of("isEmpty", "isNotEmpty", "isNull", "isNotNull", "isTrue", "isFalse").contains(operator);
        
        if (requiresValue && !condition.has("value")) {
            errors.add(new RuleValidationResult.ValidationError(
                "MISSING_VALUE", 
                "Operator '" + operator + "' requires a value", 
                path + ".value"
            ));
        }

        // Validate value constraints
        if (request.getValidateValueConstraints() && condition.has("value") && condition.has("field")) {
            String fieldName = condition.get("field").asText();
            if (availableFields.containsKey(fieldName)) {
                validateValueConstraints(condition.get("value"), availableFields.get(fieldName), path, warnings, suggestions);
            }
        }
    }

    /**
     * Validate operator compatibility with field type
     */
    private void validateOperatorCompatibility(String operator, FieldConfigEntity fieldConfig, String path,
                                             List<RuleValidationResult.ValidationError> errors,
                                             List<RuleValidationResult.ValidationWarning> warnings,
                                             List<RuleValidationResult.ValidationSuggestion> suggestions) {
        
        String fieldType = fieldConfig.getFieldType();
        List<String> validOperators = getValidOperatorsForFieldType(fieldType);
        
        if (!validOperators.contains(operator)) {
            errors.add(new RuleValidationResult.ValidationError(
                "INCOMPATIBLE_OPERATOR",
                "Operator '" + operator + "' is not compatible with field type '" + fieldType + "'",
                path + ".operator",
                fieldConfig.getFieldName(),
                "ERROR"
            ));
            
            // Suggest compatible operators
            suggestions.add(new RuleValidationResult.ValidationSuggestion(
                "COMPATIBLE_OPERATORS",
                "Compatible operators for " + fieldType + " fields: " + String.join(", ", validOperators),
                path + ".operator",
                validOperators.get(0) // Suggest the first valid operator
            ));
        }
    }

    /**
     * Validate value constraints based on field configuration
     */
    private void validateValueConstraints(JsonNode value, FieldConfigEntity fieldConfig, String path,
                                        List<RuleValidationResult.ValidationWarning> warnings,
                                        List<RuleValidationResult.ValidationSuggestion> suggestions) {
        
        String fieldType = fieldConfig.getFieldType();
        String valueStr = value.asText();
        
        switch (fieldType.toUpperCase()) {
            case "NUMBER":
                try {
                    Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    warnings.add(new RuleValidationResult.ValidationWarning(
                        "INVALID_NUMBER_FORMAT",
                        "Value '" + valueStr + "' is not a valid number",
                        path + ".value"
                    ));
                }
                break;
                
            case "DATE":
                if (!valueStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    warnings.add(new RuleValidationResult.ValidationWarning(
                        "INVALID_DATE_FORMAT",
                        "Date value should be in YYYY-MM-DD format",
                        path + ".value"
                    ));
                    suggestions.add(new RuleValidationResult.ValidationSuggestion(
                        "DATE_FORMAT",
                        "Use YYYY-MM-DD format for dates (e.g., 2024-01-15)",
                        path + ".value",
                        "2024-01-01"
                    ));
                }
                break;
                
            case "BOOLEAN":
                if (!"true".equals(valueStr) && !"false".equals(valueStr)) {
                    warnings.add(new RuleValidationResult.ValidationWarning(
                        "INVALID_BOOLEAN_FORMAT",
                        "Boolean value should be 'true' or 'false'",
                        path + ".value"
                    ));
                }
                break;
        }
    }

    /**
     * Validate field dependencies for calculated fields
     */
    private void validateFieldDependencies(JsonNode ruleNode, 
                                         List<RuleValidationResult.ValidationError> errors,
                                         List<RuleValidationResult.ValidationWarning> warnings,
                                         Map<String, FieldConfigEntity> availableFields) {
        
        Set<String> referencedFields = extractReferencedFields(ruleNode);
        
        for (String fieldName : referencedFields) {
            if (availableFields.containsKey(fieldName)) {
                FieldConfigEntity fieldConfig = availableFields.get(fieldName);
                
                // Check if calculated field has circular dependencies
                if (Boolean.TRUE.equals(fieldConfig.getIsCalculated())) {
                    warnings.add(new RuleValidationResult.ValidationWarning(
                        "CALCULATED_FIELD_DEPENDENCY",
                        "Field '" + fieldName + "' is a calculated field - ensure dependencies are resolved correctly",
                        "field." + fieldName
                    ));
                }
            }
        }
    }

    /**
     * Extract all field names referenced in a rule
     */
    private Set<String> extractReferencedFields(JsonNode ruleNode) {
        Set<String> fields = new HashSet<>();
        extractFieldsRecursive(ruleNode, fields);
        return fields;
    }

    /**
     * Recursively extract field names from rule structure
     */
    private void extractFieldsRecursive(JsonNode node, Set<String> fields) {
        if (node.has("field")) {
            fields.add(node.get("field").asText());
        }
        
        if (node.has("rules") && node.get("rules").isArray()) {
            for (JsonNode rule : node.get("rules")) {
                extractFieldsRecursive(rule, fields);
            }
        }
    }

    /**
     * Get valid operators for a field type
     */
    private List<String> getValidOperatorsForFieldType(String fieldType) {
        switch (fieldType.toUpperCase()) {
            case "STRING":
                return List.of("=", "!=", "contains", "startsWith", "endsWith", "in", "notIn", "isEmpty", "isNotEmpty");
            case "NUMBER":
                return List.of("=", "!=", "<", "<=", ">", ">=", "between", "notBetween", "in", "notIn", "isNull", "isNotNull");
            case "DATE":
                return List.of("=", "!=", "before", "after", "between", "notBetween", "in", "notIn", "isNull", "isNotNull");
            case "BOOLEAN":
                return List.of("=", "!=", "isTrue", "isFalse", "isNull", "isNotNull");
            case "ARRAY":
                return List.of("contains", "notContains", "containsAll", "containsAny", "sizeEquals", "isEmpty", "isNotEmpty", "isNull", "isNotNull");
            default:
                return List.of("=", "!=", "isNull", "isNotNull");
        }
    }

    /**
     * Find similar field name for suggestions
     */
    private String findSimilarFieldName(String input, Set<String> availableFields) {
        String inputLower = input.toLowerCase();
        
        // Look for exact case-insensitive match first
        for (String field : availableFields) {
            if (field.toLowerCase().equals(inputLower)) {
                return field;
            }
        }
        
        // Look for partial matches
        for (String field : availableFields) {
            if (field.toLowerCase().contains(inputLower) || inputLower.contains(field.toLowerCase())) {
                return field;
            }
        }
        
        // Look for similar starting characters
        for (String field : availableFields) {
            if (field.toLowerCase().startsWith(inputLower.substring(0, Math.min(3, inputLower.length())))) {
                return field;
            }
        }
        
        return null;
    }

    /**
     * Generate validation summary
     */
    private String generateValidationSummary(List<RuleValidationResult.ValidationError> errors,
                                           List<RuleValidationResult.ValidationWarning> warnings) {
        if (errors.isEmpty() && warnings.isEmpty()) {
            return "Rule validation passed successfully";
        }
        
        StringBuilder summary = new StringBuilder();
        if (!errors.isEmpty()) {
            summary.append(errors.size()).append(" error(s)");
        }
        if (!warnings.isEmpty()) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(warnings.size()).append(" warning(s)");
        }
        summary.append(" found during validation");
        
        return summary.toString();
    }
}