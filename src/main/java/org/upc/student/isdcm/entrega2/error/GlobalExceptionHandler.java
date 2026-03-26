package org.upc.student.isdcm.entrega2.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable>{

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log the exception for internal server monitoring
        LOGGER.warning(exception.getMessage());

        // Fallback message if the exception does not contain one
        String errorMessage = exception.getMessage() != null
                ? exception.getMessage()
                : "An unexpected internal server error occurred.";

        // Create the standardized payload
        ErrorResponse errorResponse = new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                errorMessage
        );

        // Return the structured response to the client
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .type("application/json")
                .build();
    }
}
