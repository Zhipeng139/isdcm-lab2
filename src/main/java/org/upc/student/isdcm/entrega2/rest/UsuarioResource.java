package org.upc.student.isdcm.entrega2.rest;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.upc.student.isdcm.entrega2.error.ErrorResponse;
import org.upc.student.isdcm.entrega2.model.Usuario;
import org.upc.student.isdcm.entrega2.repository.UsuarioRepository;

@Path("/usuaris")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @Inject
    private UsuarioRepository repo;

    @POST
    public Response registrar(Usuario usuario) {
        if (usuario == null || blank(usuario.getUsername()) || blank(usuario.getPassword()))
            return error(Response.Status.BAD_REQUEST, "username i password son obligatoris");
        try {
            String apiKey = repo.createUser(usuario);
            return Response.status(Response.Status.CREATED)
                    .entity(Json.createObjectBuilder()
                            .add("username", usuario.getUsername().toLowerCase())
                            .add("apiKey", apiKey)
                            .build())
                    .build();
        } catch (IllegalArgumentException e) {
            return error(Response.Status.CONFLICT, e.getMessage());
        }
    }

    @POST
    @Path("/login")
    public Response login(Usuario credencials) {
        if (credencials == null || blank(credencials.getUsername()) || blank(credencials.getPassword()))
            return error(Response.Status.BAD_REQUEST, "username i password son obligatoris");

        boolean valid = repo.validateCredentials(credencials.getUsername(), credencials.getPassword());
        if (!valid)
            return error(Response.Status.UNAUTHORIZED, "Credencials incorrectes");

        String apiKey = repo.getApiKey(credencials.getUsername());
        return Response.ok(
                Json.createObjectBuilder()
                        .add("username", credencials.getUsername().toLowerCase())
                        .add("apiKey", apiKey)
                        .build()
        ).build();
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(status.getStatusCode(), message))
                .build();
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
