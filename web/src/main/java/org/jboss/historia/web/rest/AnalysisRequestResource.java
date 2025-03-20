package org.jboss.historia.web.rest;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.jboss.historia.web.model.AnalysisRequestDTO;
import org.jboss.historia.web.model.RequestStatus;
import org.jboss.historia.web.service.AnalysisService;
import org.jboss.logging.Logger;

/**
 * REST resource for analysis requests.
 */
@Path("/api/requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalysisRequestResource {
    
    private static final Logger LOG = Logger.getLogger(AnalysisRequestResource.class);
    
    @Inject
    AnalysisService service;
    
    /**
     * Get all analysis requests.
     * 
     * @param securityContext The security context
     * @return A list of all requests
     */
    @GET
    @RolesAllowed({"user", "admin"})
    public List<AnalysisRequestDTO> getAllRequests(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        
        if (securityContext.isUserInRole("admin")) {
            // Admins can see all requests
            LOG.debug("Admin user " + username + " retrieving all requests");
            return service.getAllRequestDTOs();
        } else {
            // Regular users can only see their own requests
            LOG.debug("User " + username + " retrieving their requests");
            return service.getRequestDTOsByOwner(username);
        }
    }
    
    /**
     * Get an analysis request by ID.
     * 
     * @param id The ID of the request
     * @param securityContext The security context
     * @return The request, or 404 if not found
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({"user", "admin"})
    public Response getRequest(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        AnalysisRequestDTO request = service.getRequestDTOById(id);
        
        if (request == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        String username = securityContext.getUserPrincipal().getName();
        
        // Check if the user has permission to view this request
        if (!securityContext.isUserInRole("admin") && !username.equals(request.getOwner())) {
            return Response.status(Status.FORBIDDEN).build();
        }
        
        return Response.ok(request).build();
    }
    
    /**
     * Create a new analysis request.
     * 
     * @param requestDTO The request to create
     * @param securityContext The security context
     * @return The created request
     */
    @POST
    @RolesAllowed({"user", "admin"})
    public Response createRequest(AnalysisRequestDTO requestDTO, @Context SecurityContext securityContext) {
        // Validate the request
        if (requestDTO.getGitRepoUrl() == null || requestDTO.getGitRepoUrl().isEmpty()) {
            return Response.status(Status.BAD_REQUEST)
                    .entity("Git repository URL is required")
                    .build();
        }
        
        // Set the owner to the current user
        String username = securityContext.getUserPrincipal().getName();
        requestDTO.setOwner(username);
        
        // Create the request
        AnalysisRequestDTO createdRequest = AnalysisRequestDTO.fromEntity(service.createRequest(requestDTO));
        
        LOG.info("Created new analysis request: " + createdRequest.getId() + " for user " + username);
        
        return Response.status(Status.CREATED)
                .entity(createdRequest)
                .build();
    }
    
    /**
     * Cancel an analysis request.
     * 
     * @param id The ID of the request to cancel
     * @param securityContext The security context
     * @return 200 if cancelled, 404 if not found, 403 if not authorized, 400 if cannot be cancelled
     */
    @PUT
    @Path("/{id}/cancel")
    @RolesAllowed({"user", "admin"})
    public Response cancelRequest(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        AnalysisRequestDTO request = service.getRequestDTOById(id);
        
        if (request == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        String username = securityContext.getUserPrincipal().getName();
        
        // Check if the user has permission to cancel this request
        if (!securityContext.isUserInRole("admin") && !username.equals(request.getOwner())) {
            return Response.status(Status.FORBIDDEN).build();
        }
        
        // Check if the request can be cancelled
        if (request.getStatus() != RequestStatus.PENDING) {
            return Response.status(Status.BAD_REQUEST)
                    .entity("Only pending requests can be cancelled")
                    .build();
        }
        
        // Cancel the request
        boolean cancelled = service.cancelRequest(id);
        
        if (cancelled) {
            LOG.info("Cancelled analysis request: " + id + " by user " + username);
            return Response.ok().build();
        } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete an analysis request.
     * 
     * @param id The ID of the request to delete
     * @param securityContext The security context
     * @return 200 if deleted, 404 if not found, 403 if not authorized
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public Response deleteRequest(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        AnalysisRequestDTO request = service.getRequestDTOById(id);
        
        if (request == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        // Only admins can delete requests
        String username = securityContext.getUserPrincipal().getName();
        
        // Delete the request
        boolean deleted = service.deleteRequest(id);
        
        if (deleted) {
            LOG.info("Deleted analysis request: " + id + " by admin " + username);
            return Response.ok().build();
        } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get the result of an analysis request.
     * 
     * @param id The ID of the request
     * @param securityContext The security context
     * @return The result as CSV, or 404 if not found, 403 if not authorized, 400 if not completed
     */
    @GET
    @Path("/{id}/result")
    @Produces("text/csv")
    @RolesAllowed({"user", "admin"})
    public Response getRequestResult(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        AnalysisRequestDTO request = service.getRequestDTOById(id);
        
        if (request == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        String username = securityContext.getUserPrincipal().getName();
        
        // Check if the user has permission to view this request
        if (!securityContext.isUserInRole("admin") && !username.equals(request.getOwner())) {
            return Response.status(Status.FORBIDDEN).build();
        }
        
        // Check if the request is completed
        if (request.getStatus() != RequestStatus.COMPLETED) {
            return Response.status(Status.BAD_REQUEST)
                    .entity("Request is not completed")
                    .build();
        }
        
        // Get the result
        String result = service.getResultFileContent(id);
        
        if (result == null) {
            return Response.status(Status.NOT_FOUND)
                    .entity("Result file not found")
                    .build();
        }
        
        return Response.ok(result)
                .header("Content-Disposition", "attachment; filename=\"analysis-" + id + ".csv\"")
                .build();
    }
}
