package org.jboss.historia.web.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.historia.web.model.AnalysisRequest;
import org.jboss.historia.web.model.RequestStatus;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

/**
 * Repository for AnalysisRequest entities.
 */
@ApplicationScoped
public class AnalysisRequestRepository implements PanacheRepository<AnalysisRequest> {
    
    /**
     * Find all requests with the given status.
     * 
     * @param status The status to filter by
     * @return A list of requests with the given status
     */
    public List<AnalysisRequest> findByStatus(RequestStatus status) {
        return list("status", status);
    }
    
    /**
     * Find all pending requests.
     * 
     * @return A list of pending requests
     */
    public List<AnalysisRequest> findPending() {
        return findByStatus(RequestStatus.PENDING);
    }
    
    /**
     * Find all processing requests.
     * 
     * @return A list of processing requests
     */
    public List<AnalysisRequest> findProcessing() {
        return findByStatus(RequestStatus.PROCESSING);
    }
    
    /**
     * Find all completed requests.
     * 
     * @return A list of completed requests
     */
    public List<AnalysisRequest> findCompleted() {
        return findByStatus(RequestStatus.COMPLETED);
    }
    
    /**
     * Find all failed requests.
     * 
     * @return A list of failed requests
     */
    public List<AnalysisRequest> findFailed() {
        return findByStatus(RequestStatus.FAILED);
    }
    
    /**
     * Find all requests owned by the given user.
     * 
     * @param owner The owner to filter by
     * @return A list of requests owned by the given user
     */
    public List<AnalysisRequest> findByOwner(String owner) {
        return list("owner", owner);
    }
    
    /**
     * Find all requests owned by the given user with the given status.
     * 
     * @param owner The owner to filter by
     * @param status The status to filter by
     * @return A list of requests owned by the given user with the given status
     */
    public List<AnalysisRequest> findByOwnerAndStatus(String owner, RequestStatus status) {
        return list("owner = ?1 and status = ?2", owner, status);
    }
    
    /**
     * Find all requests for the given git repository URL.
     * 
     * @param gitRepoUrl The git repository URL to filter by
     * @return A list of requests for the given git repository URL
     */
    public List<AnalysisRequest> findByGitRepoUrl(String gitRepoUrl) {
        return list("gitRepoUrl", gitRepoUrl);
    }
    
    /**
     * Count the number of requests with the given status.
     * 
     * @param status The status to count
     * @return The number of requests with the given status
     */
    public long countByStatus(RequestStatus status) {
        return count("status", status);
    }
}
