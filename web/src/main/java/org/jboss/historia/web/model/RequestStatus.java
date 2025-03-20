package org.jboss.historia.web.model;

/**
 * Enum representing the status of an analysis request.
 */
public enum RequestStatus {
    /**
     * The request has been submitted but not yet processed.
     */
    PENDING,
    
    /**
     * The request is currently being processed.
     */
    PROCESSING,
    
    /**
     * The request has been completed successfully.
     */
    COMPLETED,
    
    /**
     * The request has failed.
     */
    FAILED,
    
    /**
     * The request has been cancelled by the user or administrator.
     */
    CANCELLED
}
