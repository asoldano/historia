package org.jboss.historia.web.model;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for AnalysisRequest.
 * Used for transferring data between the client and server.
 */
public class AnalysisRequestDTO {
    
    private Long id;
    private String gitRepoUrl;
    private String pathFilter;
    private String localRepoCloneUri;
    private RequestStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String resultFilePath;
    private String errorMessage;
    private String owner;
    private Long durationSeconds;
    
    // Default constructor
    public AnalysisRequestDTO() {
    }
    
    /**
     * Create a DTO from an entity.
     * 
     * @param entity The entity to convert
     * @return A new DTO with data from the entity
     */
    public static AnalysisRequestDTO fromEntity(AnalysisRequest entity) {
        AnalysisRequestDTO dto = new AnalysisRequestDTO();
        dto.setId(entity.getId());
        dto.setGitRepoUrl(entity.getGitRepoUrl());
        dto.setPathFilter(entity.getPathFilter());
        dto.setLocalRepoCloneUri(entity.getLocalRepoCloneUri());
        dto.setStatus(entity.getStatus());
        dto.setSubmittedAt(entity.getSubmittedAt());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setResultFilePath(entity.getResultFilePath());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setOwner(entity.getOwner());
        dto.setDurationSeconds(entity.getDurationSeconds());
        return dto;
    }
    
    /**
     * Convert this DTO to an entity.
     * 
     * @return A new entity with data from this DTO
     */
    public AnalysisRequest toEntity() {
        AnalysisRequest entity = new AnalysisRequest();
        entity.setId(this.getId());
        entity.setGitRepoUrl(this.getGitRepoUrl());
        entity.setPathFilter(this.getPathFilter());
        entity.setLocalRepoCloneUri(this.getLocalRepoCloneUri());
        entity.setStatus(this.getStatus());
        entity.setSubmittedAt(this.getSubmittedAt());
        entity.setStartedAt(this.getStartedAt());
        entity.setCompletedAt(this.getCompletedAt());
        entity.setResultFilePath(this.getResultFilePath());
        entity.setErrorMessage(this.getErrorMessage());
        entity.setOwner(this.getOwner());
        return entity;
    }
    
    // Getters and setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getGitRepoUrl() {
        return gitRepoUrl;
    }
    
    public void setGitRepoUrl(String gitRepoUrl) {
        this.gitRepoUrl = gitRepoUrl;
    }
    
    public String getPathFilter() {
        return pathFilter;
    }
    
    public void setPathFilter(String pathFilter) {
        this.pathFilter = pathFilter;
    }
    
    public String getLocalRepoCloneUri() {
        return localRepoCloneUri;
    }
    
    public void setLocalRepoCloneUri(String localRepoCloneUri) {
        this.localRepoCloneUri = localRepoCloneUri;
    }
    
    public RequestStatus getStatus() {
        return status;
    }
    
    public void setStatus(RequestStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getResultFilePath() {
        return resultFilePath;
    }
    
    public void setResultFilePath(String resultFilePath) {
        this.resultFilePath = resultFilePath;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public Long getDurationSeconds() {
        return durationSeconds;
    }
    
    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
