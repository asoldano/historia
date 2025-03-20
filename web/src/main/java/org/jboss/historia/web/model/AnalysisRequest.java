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
        this.localRepoCloneUri = "target/jgit/" + repoName;
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
    
    @Override
    public String toString() {
        return "AnalysisRequest [id=" + id + ", gitRepoUrl=" + gitRepoUrl + ", status=" + status + "]";
    }
}
