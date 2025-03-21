package org.jboss.historia.web.controller;

import java.io.BufferedReader;
import java.io.StringReader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.jboss.historia.web.model.AnalysisRequest;
import org.jboss.historia.web.model.AnalysisRequestDTO;
import org.jboss.historia.web.model.RequestStatus;
import org.jboss.historia.web.service.AnalysisService;
import org.jboss.logging.Logger;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Controller for web UI pages.
 */
@Path("/")
public class WebController {
    
    private static final Logger LOG = Logger.getLogger(WebController.class);
    
    @Inject
    AnalysisService service;
    
    @Inject
    SecurityIdentity identity;
    
    @Inject
    @io.quarkus.qute.Location("index.html")
    io.quarkus.qute.Template indexTemplate;
    
    @Inject
    @io.quarkus.qute.Location("requests.html")
    io.quarkus.qute.Template requestsTemplate;
    
    @Inject
    @io.quarkus.qute.Location("new-request.html")
    io.quarkus.qute.Template newRequestTemplate;
    
    @Inject
    @io.quarkus.qute.Location("request-details.html")
    io.quarkus.qute.Template requestDetailsTemplate;
    
    @Inject
    @io.quarkus.qute.Location("visualization.html")
    io.quarkus.qute.Template visualizationTemplate;
    
    /**
     * Home page.
     * 
     * @param securityContext The security context
     * @return The home page template
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(@Context SecurityContext securityContext) {
        return indexTemplate.data("user", getUserInfo(securityContext))
                           .data("active", "home");
    }
    
    /**
     * Requests list page.
     * 
     * @param securityContext The security context
     * @param messageText The message text (optional)
     * @param messageType The message type (optional)
     * @return The requests list template
     */
    @GET
    @Path("/requests")
    @Produces(MediaType.TEXT_HTML)
    @RolesAllowed({"user", "admin"})
    public TemplateInstance requests(@Context SecurityContext securityContext,
                                   @QueryParam("message") String messageText,
                                   @QueryParam("type") String messageType) {
        String username = securityContext.getUserPrincipal().getName();
        boolean isAdmin = securityContext.isUserInRole("admin");
        
        List<AnalysisRequestDTO> requests;
        if (isAdmin) {
            // Admins can see all requests
            requests = service.getAllRequestDTOs();
        } else {
            // Regular users can only see their own requests
            requests = service.getRequestDTOsByOwner(username);
        }
        
        // Filter requests by status
        List<AnalysisRequestDTO> pendingRequests = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .collect(Collectors.toList());
        
        List<AnalysisRequestDTO> processingRequests = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.PROCESSING)
                .collect(Collectors.toList());
        
        List<AnalysisRequestDTO> completedRequests = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.COMPLETED)
                .collect(Collectors.toList());
        
        List<AnalysisRequestDTO> failedRequests = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.FAILED)
                .collect(Collectors.toList());
        
        // Create message if provided
        Map<String, Object> message = null;
        if (messageText != null && !messageText.isEmpty()) {
            message = new HashMap<>();
            message.put("text", messageText);
            message.put("type", messageType != null ? messageType : "info");
        }
        
        return requestsTemplate.data("requests", requests)
                              .data("pendingRequests", pendingRequests)
                              .data("processingRequests", processingRequests)
                              .data("completedRequests", completedRequests)
                              .data("failedRequests", failedRequests)
                              .data("pendingCount", pendingRequests.size())
                              .data("processingCount", processingRequests.size())
                              .data("completedCount", completedRequests.size())
                              .data("failedCount", failedRequests.size())
                              .data("user", getUserInfo(securityContext))
                              .data("active", "requests")
                              .data("message", message);
    }
    
    /**
     * New request page.
     * 
     * @param securityContext The security context
     * @return The new request template
     */
    @GET
    @Path("/requests/new")
    @Produces(MediaType.TEXT_HTML)
    @RolesAllowed({"user", "admin"})
    public TemplateInstance newRequest(@Context SecurityContext securityContext) {
        return newRequestTemplate.data("user", getUserInfo(securityContext))
                                .data("active", "new-request");
    }
    
    /**
     * Request details page.
     * 
     * @param id The request ID
     * @param securityContext The security context
     * @return The request details template
     */
    @GET
    @Path("/requests/{id}")
    @Produces(MediaType.TEXT_HTML)
    @RolesAllowed({"user", "admin"})
    public TemplateInstance requestDetails(@PathParam("id") Long id,
                                         @Context SecurityContext securityContext) {
        AnalysisRequestDTO request = service.getRequestDTOById(id);
        if (request == null) {
            return indexTemplate.data("user", getUserInfo(securityContext))
                               .data("active", "home");
        }
        
        String username = securityContext.getUserPrincipal().getName();
        boolean isAdmin = securityContext.isUserInRole("admin");
        
        // Check if the user has permission to view this request
        if (!isAdmin && !username.equals(request.getOwner())) {
            return indexTemplate.data("user", getUserInfo(securityContext))
                               .data("active", "home");
        }
        
        // Get result preview if completed
        List<List<String>> resultPreview = null;
        List<String> resultHeaders = null;
        boolean resultHasMore = false;
        
        if (request.getStatus() == RequestStatus.COMPLETED) {
            String resultContent = service.getResultFileContent(id);
            if (resultContent != null) {
                try (BufferedReader reader = new BufferedReader(new StringReader(resultContent))) {
                    // Read headers
                    String headerLine = reader.readLine();
                    if (headerLine != null) {
                        resultHeaders = List.of(headerLine.split(","));
                        
                        // Read up to 10 rows for preview
                        resultPreview = new ArrayList<>();
                        String line;
                        int count = 0;
                        while ((line = reader.readLine()) != null && count < 10) {
                            resultPreview.add(List.of(line.split(",")));
                            count++;
                        }
                        
                        // Check if there are more rows
                        resultHasMore = reader.readLine() != null;
                    }
                } catch (Exception e) {
                    LOG.error("Error reading result file", e);
                }
            }
        }
        
        return requestDetailsTemplate.data("request", request)
                                    .data("resultPreview", resultPreview)
                                    .data("resultHeaders", resultHeaders)
                                    .data("resultHasMore", resultHasMore)
                                    .data("user", getUserInfo(securityContext))
                                    .data("active", "requests");
    }
    
    /**
     * Visualization page.
     * 
     * @param id The request ID
     * @param securityContext The security context
     * @return The visualization template
     */
    @GET
    @Path("/requests/{id}/visualization")
    @Produces(MediaType.TEXT_HTML)
    @RolesAllowed({"user", "admin"})
    public TemplateInstance visualization(@PathParam("id") Long id,
                                        @Context SecurityContext securityContext) {
        AnalysisRequestDTO request = service.getRequestDTOById(id);
        if (request == null) {
            return indexTemplate.data("user", getUserInfo(securityContext))
                               .data("active", "home");
        }
        
        String username = securityContext.getUserPrincipal().getName();
        boolean isAdmin = securityContext.isUserInRole("admin");
        
        // Check if the user has permission to view this request
        if (!isAdmin && !username.equals(request.getOwner())) {
            return indexTemplate.data("user", getUserInfo(securityContext))
                               .data("active", "home");
        }
        
        // Check if the request is completed
        if (request.getStatus() != RequestStatus.COMPLETED) {
            return requestDetailsTemplate.data("request", request)
                                        .data("resultPreview", null)
                                        .data("resultHeaders", null)
                                        .data("resultHasMore", false)
                                        .data("user", getUserInfo(securityContext))
                                        .data("active", "requests");
        }
        
        return visualizationTemplate.data("request", request)
                                   .data("user", getUserInfo(securityContext))
                                   .data("active", "requests");
    }
    
    /**
     * Get user info for templates.
     * 
     * @param securityContext The security context
     * @return A map with user info
     */
    private Map<String, Object> getUserInfo(SecurityContext securityContext) {
        Map<String, Object> user = new HashMap<>();
        
        Principal principal = securityContext.getUserPrincipal();
        if (principal != null) {
            user.put("name", principal.getName());
            user.put("isAdmin", securityContext.isUserInRole("admin"));
        }
        
        return user;
    }
    
}
