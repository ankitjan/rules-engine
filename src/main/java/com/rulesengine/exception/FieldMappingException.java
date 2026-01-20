package com.rulesengine.exception;

/**
 * Exception thrown when field mapping operations fail.
 * Provides detailed information about the mapping failure including
 * the mapper expression and the path where the failure occurred.
 */
public class FieldMappingException extends RuntimeException {
    
    private final String mapperExpression;
    private final String failingPath;
    
    /**
     * Creates a new FieldMappingException with a message, mapper expression, and failing path.
     * 
     * @param message The error message
     * @param mapperExpression The mapper expression that failed
     * @param failingPath The path within the expression where the failure occurred
     */
    public FieldMappingException(String message, String mapperExpression, String failingPath) {
        super(buildDetailedMessage(message, mapperExpression, failingPath));
        this.mapperExpression = mapperExpression;
        this.failingPath = failingPath;
    }
    
    /**
     * Creates a new FieldMappingException with a message, mapper expression, failing path, and cause.
     * 
     * @param message The error message
     * @param mapperExpression The mapper expression that failed
     * @param failingPath The path within the expression where the failure occurred
     * @param cause The underlying cause of the exception
     */
    public FieldMappingException(String message, String mapperExpression, String failingPath, Throwable cause) {
        super(buildDetailedMessage(message, mapperExpression, failingPath), cause);
        this.mapperExpression = mapperExpression;
        this.failingPath = failingPath;
    }
    
    /**
     * Gets the mapper expression that caused the failure.
     * 
     * @return The mapper expression, or null if not applicable
     */
    public String getMapperExpression() {
        return mapperExpression;
    }
    
    /**
     * Gets the path within the mapper expression where the failure occurred.
     * 
     * @return The failing path
     */
    public String getFailingPath() {
        return failingPath;
    }
    
    /**
     * Builds a detailed error message including the mapper expression and failing path.
     */
    private static String buildDetailedMessage(String message, String mapperExpression, String failingPath) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        
        if (mapperExpression != null) {
            sb.append(" | Expression: '").append(mapperExpression).append("'");
        }
        
        if (failingPath != null && !failingPath.equals("root") && !failingPath.equals("unknown")) {
            sb.append(" | Failed at: '").append(failingPath).append("'");
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a user-friendly error message for API responses.
     * 
     * @return A formatted error message suitable for client consumption
     */
    public String getUserFriendlyMessage() {
        StringBuilder sb = new StringBuilder();
        
        if (mapperExpression != null) {
            sb.append("Field mapping failed for expression '").append(mapperExpression).append("'");
            
            if (failingPath != null && !failingPath.equals("root") && !failingPath.equals("unknown")) {
                sb.append(" at path '").append(failingPath).append("'");
            }
            
            sb.append(". ");
        }
        
        // Extract the core error message without technical details
        String originalMessage = super.getMessage();
        if (originalMessage != null) {
            int pipeIndex = originalMessage.indexOf(" | ");
            if (pipeIndex > 0) {
                sb.append(originalMessage.substring(0, pipeIndex));
            } else {
                sb.append(originalMessage);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Gets suggestions for resolving the mapping error based on the failure type.
     * 
     * @return A list of suggestions for fixing the mapping issue
     */
    public String getSuggestion() {
        String message = getMessage().toLowerCase();
        
        if (message.contains("null value")) {
            return "Check if the data source returns the expected structure and ensure all intermediate objects exist.";
        }
        
        if (message.contains("property") && message.contains("not found")) {
            return "Verify the property name exists in the response object. Check for typos or case sensitivity.";
        }
        
        if (message.contains("array index") && message.contains("out of bounds")) {
            return "Ensure the array has enough elements or use conditional logic to check array size first.";
        }
        
        if (message.contains("cannot convert")) {
            return "Check the data type of the source value and ensure it's compatible with the target type.";
        }
        
        if (message.contains("invalid array expression")) {
            return "Use valid array syntax: [0] for indexing, [field=value] for filtering, or [*] for iteration.";
        }
        
        if (message.contains("map does not contain key")) {
            return "Verify the key exists in the response data or use optional mapping with default values.";
        }
        
        return "Review the mapper expression syntax and ensure it matches the actual response structure.";
    }
}