package org.jboss.historia.web.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

/**
 * Entity representing an analysis request.
 */
@Entity
@Table(name = "analysis_requests")
public class AnalysisRequest extends PanacheEntityBase {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(nullable = false)
    private String gitRepoUrl;
    
    @Column
    private String pathFilter;
    
    @Column
    private String localRepoCloneUri;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestStatus status;
    
    @Column(nullable = false)
    private LocalDateTime submittedAt;
    
    @Column
    private LocalDateTime startedAt;
    
    @Column
    private LocalDateTime completedAt;
    
    @Column
    private String resultFilePath;
    
    @Column(length = 1000)
    private String errorMessage;
    
    @Column
    private String owner;
    
    // Default constructor required by JPA
    public AnalysisRequest() {
    }
    
    /**
     * Create a new analysis request with the given parameters.
     * 
     * @param gitRepoUrl The URL of the git repository to analyze
     * @param pathFilter The path filter to apply (optional)
     * @param owner The owner of the request
     */
    public AnalysisRequest(String gitRepoUrl, String pathFilter, String owner) {
        this.gitRepoUrl = gitRepoUrl;
        this.pathFilter = pathFilter;
        this.owner = owner;
        this.status = RequestStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
        
        // Generate a default local repo clone URI based on the git repo URL
        String repoName = gitRepoUrl.substring(gitRepoUrl.lastIndexOf('/') + 1);
        if (repoName.endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4);
        }
        // Use absolute path to avoid issues with relative paths
        this.localRepoCloneUri = System.getProperty("user.dir") + "/target/historia-web-app/jgit/" + submittedAt + "/" + repoName;
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
    
    /**
     * Calculate the duration of the analysis in seconds.
     * 
     * @return The duration in seconds, or null if the analysis is not complete
     */
    public Long getDurationSeconds() {
        if (startedAt != null && completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
        return null;
    }
    
    /**
     * Get the hours part of the duration.
     * 
     * @return The hours part of the duration, or null if the analysis is not complete
     */
    public Long getDurationHours() {
        Long seconds = getDurationSeconds();
        if (seconds != null) {
            return seconds / 3600;
        }
        return null;
    }
    
    /**
     * Get the minutes part of the duration (excluding full hours).
     * 
     * @return The minutes part of the duration, or null if the analysis is not complete
     */
    public Long getDurationMinutes() {
        Long seconds = getDurationSeconds();
        if (seconds != null) {
            return (seconds % 3600) / 60;
        }
        return null;
    }
    
    /**
     * Get the seconds part of the duration (excluding full minutes).
     * 
     * @return The seconds part of the duration, or null if the analysis is not complete
     */
    public Long getDurationRemainingSeconds() {
        Long seconds = getDurationSeconds();
        if (seconds != null) {
            return seconds % 60;
        }
        return null;
    }
    
    /**
     * Get a formatted string representation of the duration.
     * 
     * @return A formatted string representation of the duration (e.g., "1h 30m 45s"), or null if the analysis is not complete
     */
    public String getFormattedDuration() {
        Long seconds = getDurationSeconds();
        if (seconds == null) {
            return null;
        }
        
        if (seconds > 3600) {
            return getDurationHours() + "h " + getDurationMinutes() + "m " + getDurationRemainingSeconds() + "s";
        } else if (seconds > 60) {
            return getDurationMinutes() + "m " + getDurationRemainingSeconds() + "s";
        } else {
            return seconds + "s";
        }
    }
    
    @Override
    public String toString() {
        return "AnalysisRequest [id=" + id + ", gitRepoUrl=" + gitRepoUrl + ", status=" + status + "]";
    }
}
