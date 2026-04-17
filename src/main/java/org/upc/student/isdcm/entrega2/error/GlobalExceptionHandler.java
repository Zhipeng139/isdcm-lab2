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

        LOGGER.warning(exception.getMessage());

        if (exception instanceof jakarta.ws.rs.NotAllowedException) {
            return build(Response.Status.METHOD_NOT_ALLOWED, exception);
        }

        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            return build(Response.Status.NOT_FOUND, exception);
        }

        if (exception instanceof jakarta.ws.rs.BadRequestException) {
            return build(Response.Status.BAD_REQUEST, exception);
        }

        // fallback
        return build(Response.Status.INTERNAL_SERVER_ERROR, exception);
    }

    private Response build(Response.Status status, Throwable ex) {
        String msg = ex.getMessage() != null
                ? ex.getMessage()
                : "Unexpected error";

        ErrorResponse errorResponse = new ErrorResponse(
                status.getStatusCode(),
                msg
        );

        return Response.status(status)
                .entity(errorResponse)
                .type("application/json")
                .build();
    }
}