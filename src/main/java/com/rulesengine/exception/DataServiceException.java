package com.rulesengine.exception;

/**
 * Exception thrown when data service operations fail.
 */
public class DataServiceException extends RuntimeException {
    
    public DataServiceException(String message) {
        super(message);
    }
    
    public DataServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}