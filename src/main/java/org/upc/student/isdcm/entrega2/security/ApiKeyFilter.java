package org.upc.student.isdcm.entrega2.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.upc.student.isdcm.entrega2.error.ErrorResponse;
import org.upc.student.isdcm.entrega2.repository.UsuarioRepository;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    @Inject
    private UsuarioRepository repo;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String apiKey = ctx.getHeaderString("X-API-Key");
        if (apiKey == null || apiKey.isBlank() || !repo.validateApiKey(apiKey)) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse(401, "API key invàlida o absent"))
                    .build());
        }
    }
}
