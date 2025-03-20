package org.jboss.historia.web.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.historia.core.UntestedCommitDetectionStrategy;
import org.jboss.historia.web.model.AnalysisRequest;
import org.jboss.historia.web.model.AnalysisRequestDTO;
import org.jboss.historia.web.model.RequestStatus;
import org.jboss.historia.web.repository.AnalysisRequestRepository;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;

/**
 * Service for managing analysis requests.
 */
@ApplicationScoped
public class AnalysisService {
    
    private static final Logger LOG = Logger.getLogger(AnalysisService.class);
    
    @Inject
    AnalysisRequestRepository repository;
    
    @ConfigProperty(name = "historia.results.directory")
    String resultsDirectory;
    
    @ConfigProperty(name = "historia.max.concurrent.analyses", defaultValue = "2")
    int maxConcurrentAnalyses;
    
    private final AtomicInteger activeProcesses = new AtomicInteger(0);
    
    /**
     * Create a new analysis request.
     * 
     * @param gitRepoUrl The URL of the git repository to analyze
     * @param pathFilter The path filter to apply (optional)
     * @param owner The owner of the request
     * @return The created request
     */
    @Transactional
    public AnalysisRequest createRequest(String gitRepoUrl, String pathFilter, String owner) {
        AnalysisRequest request = new AnalysisRequest(gitRepoUrl, pathFilter, owner);
        repository.persist(request);
        LOG.info("Created new analysis request: " + request.getId());
        return request;
    }
    
    /**
     * Create a new analysis request from a DTO.
     * 
     * @param dto The DTO containing the request data
     * @return The created request
     */
    @Transactional
    public AnalysisRequest createRequest(AnalysisRequestDTO dto) {
        return createRequest(dto.getGitRepoUrl(), dto.getPathFilter(), dto.getOwner());
    }
    
    /**
     * Get all analysis requests.
     * 
     * @return A list of all requests
     */
    public List<AnalysisRequest> getAllRequests() {
        return repository.listAll();
    }
    
    /**
     * Get all analysis requests as DTOs.
     * 
     * @return A list of all requests as DTOs
     */
    public List<AnalysisRequestDTO> getAllRequestDTOs() {
        return repository.listAll().stream()
                .map(AnalysisRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get an analysis request by ID.
     * 
     * @param id The ID of the request
     * @return The request, or null if not found
     */
    public AnalysisRequest getRequestById(Long id) {
        return repository.findById(id);
    }
    
    /**
     * Get an analysis request by ID as a DTO.
     * 
     * @param id The ID of the request
     * @return The request as a DTO, or null if not found
     */
    public AnalysisRequestDTO getRequestDTOById(Long id) {
        AnalysisRequest entity = repository.findById(id);
        return entity != null ? AnalysisRequestDTO.fromEntity(entity) : null;
    }
    
    /**
     * Get all requests owned by the given user.
     * 
     * @param owner The owner to filter by
     * @return A list of requests owned by the given user
     */
    public List<AnalysisRequest> getRequestsByOwner(String owner) {
        return repository.findByOwner(owner);
    }
    
    /**
     * Get all requests owned by the given user as DTOs.
     * 
     * @param owner The owner to filter by
     * @return A list of requests owned by the given user as DTOs
     */
    public List<AnalysisRequestDTO> getRequestDTOsByOwner(String owner) {
        return repository.findByOwner(owner).stream()
                .map(AnalysisRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Process queued analysis requests.
     * This method is scheduled to run periodically.
     */
    @Scheduled(every = "{historia.scheduler.interval}s")
    void processQueuedRequests() {
        LOG.info("Checking for pending analysis requests...");
        
        if (!canStartNewAnalysis()) {
            LOG.info("Maximum number of concurrent analyses reached. Skipping this cycle.");
            return;
        }
        
        List<AnalysisRequest> pendingRequests = repository.findPending();
        
        if (pendingRequests.isEmpty()) {
            LOG.info("No pending requests found.");
            return;
        }
        
        LOG.info("Found " + pendingRequests.size() + " pending requests.");
        
        // Process only one request at a time to avoid overloading the system
        AnalysisRequest request = pendingRequests.get(0);
        processRequest(request);
    }
    
    /**
     * Check if a new analysis can be started.
     * 
     * @return true if a new analysis can be started, false otherwise
     */
    private boolean canStartNewAnalysis() {
        return activeProcesses.get() < maxConcurrentAnalyses;
    }
    
    /**
     * Process an analysis request.
     * 
     * @param request The request to process
     */
    @Transactional
    public void processRequest(AnalysisRequest request) {
        LOG.info("Processing request: " + request.getId());
        
        try {
            // Update status to PROCESSING
            request.setStatus(RequestStatus.PROCESSING);
            request.setStartedAt(LocalDateTime.now());
            repository.persist(request);
            
            // Increment active processes counter
            activeProcesses.incrementAndGet();
            
            // Create results directory if it doesn't exist
            Path resultsPath = Paths.get(resultsDirectory);
            if (!Files.exists(resultsPath)) {
                Files.createDirectories(resultsPath);
            }
            
            // Create a unique output file path
            String outputFileName = "analysis-" + request.getId() + "-" + 
                                   System.currentTimeMillis() + ".csv";
            String outputFilePath = Paths.get(resultsDirectory, outputFileName).toString();
            
            // Run the analysis asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    runAnalysis(request, outputFilePath);
                    
                    // Update status to COMPLETED
                    updateRequestStatus(request.getId(), RequestStatus.COMPLETED, 
                                       outputFilePath, null);
                    
                } catch (Exception e) {
                    LOG.error("Error processing request: " + request.getId(), e);
                    
                    // Update status to FAILED
                    updateRequestStatus(request.getId(), RequestStatus.FAILED, 
                                       null, e.getMessage());
                } finally {
                    // Decrement active processes counter
                    activeProcesses.decrementAndGet();
                }
            });
            
        } catch (Exception e) {
            LOG.error("Error starting request processing: " + request.getId(), e);
            request.setStatus(RequestStatus.FAILED);
            request.setErrorMessage("Failed to start processing: " + e.getMessage());
            repository.persist(request);
            
            // Decrement active processes counter
            activeProcesses.decrementAndGet();
        }
    }
    
    /**
     * Run the analysis for a request.
     * 
     * @param request The request to analyze
     * @param outputFilePath The path to write the output to
     * @throws Exception If an error occurs during analysis
     */
    private void runAnalysis(AnalysisRequest request, String outputFilePath) throws Exception {
        LOG.info("Running analysis for request: " + request.getId());
        
        // Create the local repo clone directory if it doesn't exist
        File localRepoDir = new File(request.getLocalRepoCloneUri());
        if (!localRepoDir.exists()) {
            localRepoDir.mkdirs();
        }
        
        // Use the core module to run the analysis
        UntestedCommitDetectionStrategy strategy = 
            new UntestedCommitDetectionStrategy(request.getPathFilter());
            
        try (FileWriter writer = new FileWriter(outputFilePath);
             BufferedWriter bw = new BufferedWriter(writer)) {
            
            strategy.process(
                request.getGitRepoUrl(),
                request.getLocalRepoCloneUri(),
                bw
            );
        }
        
        LOG.info("Analysis completed for request: " + request.getId());
    }
    
    /**
     * Update the status of a request.
     * 
     * @param requestId The ID of the request
     * @param status The new status
     * @param resultFilePath The path to the result file (if completed)
     * @param errorMessage The error message (if failed)
     */
    @Transactional
    public void updateRequestStatus(Long requestId, RequestStatus status, 
                                   String resultFilePath, String errorMessage) {
        AnalysisRequest request = repository.findById(requestId);
        if (request != null) {
            request.setStatus(status);
            request.setCompletedAt(LocalDateTime.now());
            
            if (resultFilePath != null) {
                request.setResultFilePath(resultFilePath);
            }
            
            if (errorMessage != null) {
                request.setErrorMessage(errorMessage);
            }
            
            repository.persist(request);
            LOG.info("Updated request status: " + requestId + " -> " + status);
        }
    }
    
    /**
     * Cancel a request.
     * 
     * @param requestId The ID of the request to cancel
     * @return true if the request was cancelled, false otherwise
     */
    @Transactional
    public boolean cancelRequest(Long requestId) {
        AnalysisRequest request = repository.findById(requestId);
        if (request != null && request.getStatus() == RequestStatus.PENDING) {
            request.setStatus(RequestStatus.CANCELLED);
            request.setCompletedAt(LocalDateTime.now());
            repository.persist(request);
            LOG.info("Cancelled request: " + requestId);
            return true;
        }
        return false;
    }
    
    /**
     * Delete a request.
     * 
     * @param requestId The ID of the request to delete
     * @return true if the request was deleted, false otherwise
     */
    @Transactional
    public boolean deleteRequest(Long requestId) {
        AnalysisRequest request = repository.findById(requestId);
        if (request != null) {
            // Delete the result file if it exists
            if (request.getResultFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(request.getResultFilePath()));
                } catch (Exception e) {
                    LOG.warn("Failed to delete result file: " + request.getResultFilePath(), e);
                }
            }
            
            repository.delete(request);
            LOG.info("Deleted request: " + requestId);
            return true;
        }
        return false;
    }
    
    /**
     * Get the content of a result file.
     * 
     * @param requestId The ID of the request
     * @return The content of the result file, or null if not found
     */
    public String getResultFileContent(Long requestId) {
        AnalysisRequest request = repository.findById(requestId);
        if (request != null && request.getResultFilePath() != null) {
            try {
                return Files.readString(Paths.get(request.getResultFilePath()));
            } catch (Exception e) {
                LOG.error("Failed to read result file: " + request.getResultFilePath(), e);
            }
        }
        return null;
    }
}
