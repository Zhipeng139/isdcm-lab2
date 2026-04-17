package org.upc.student.isdcm.entrega2.error;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException wae) {
            return wae.getResponse();
        }

        LOGGER.warning(exception.getMessage());

        String errorMessage = exception.getMessage() != null
                ? exception.getMessage()
                : "An unexpected internal server error occurred.";

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), errorMessage))
                .type("application/json")
                .build();
    }
}
