package org.upc.student.isdcm.entrega2.rest;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/health")
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        JsonObject jsonPayload = Json.createObjectBuilder()
                .add("status", "true")
                .build();

        return Response.ok(jsonPayload).build();
    }
}