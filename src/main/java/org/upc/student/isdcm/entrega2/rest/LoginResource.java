package org.upc.student.isdcm.entrega2.rest;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.upc.student.isdcm.entrega2.error.ErrorResponse;
import org.upc.student.isdcm.entrega2.repository.UsuarioRepository;
import org.upc.student.isdcm.entrega2.security.JwtUtil;

@Path("/")
public class LoginResource {

    @Inject
    private UsuarioRepository repo;

    @Path("login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response Login(@FormParam("username") String username,
                          @FormParam("password") String password) {

        if (blank(username) || blank(password))
            return error(Response.Status.BAD_REQUEST, "username i password son obligatoris");

        if (!repo.validateCredentials(username, password))
            return error(Response.Status.UNAUTHORIZED, "Credencials incorrectes");

        String apiKey = repo.getApiKey(username);
        String jwt = JwtUtil.createToken(username.toLowerCase(), apiKey);
        String jwe = JwtUtil.wrapInJwe(jwt);
        return Response.ok(
                Json.createObjectBuilder()
                        .add("username", username.toLowerCase())
                        .add("apiKey", apiKey)
                        .add("token", jwt)
                        .add("tokenJwe", jwe)
                        .add("jwsSecret", JwtUtil.getSecret())
                        .add("jweKey", JwtUtil.jweKeyBase64Url())
                        .add("jweJwk", JwtUtil.jweKeyAsJwk())
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
