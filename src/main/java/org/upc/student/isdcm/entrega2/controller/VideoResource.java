package org.upc.student.isdcm.entrega2.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.upc.student.isdcm.entrega2.error.ErrorResponse;
import org.upc.student.isdcm.entrega2.model.Video;
import org.upc.student.isdcm.entrega2.repository.VideoRepository;

import java.util.List;

@Path("/videos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VideoResource {

    @Inject
    private VideoRepository repo;

    @GET
    public Response obtenirTots() {
        return Response.ok(repo.findAll()).build();
    }

    @POST
    public Response crear(Video video) {
        try {
            repo.crear(video);
            return Response.status(Response.Status.CREATED).entity(video).build();
        } catch (IllegalArgumentException e) {
            return error(Response.Status.CONFLICT, e.getMessage());
        }
    }

    @POST
    @Path("/{id}/view")
    public Response reproducir(@PathParam("id") String id) {
        try {
            repo.incrementarReproducciones(id);
            return Response.ok(repo.findById(id)).build();
        } catch (IllegalArgumentException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") String id, Video video) {
        if (repo.findById(id) == null)
            return error(Response.Status.NOT_FOUND, "Video not found: " + id);
        video.setId(id);
        return Response.ok(repo.actualizar(video)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") String id) {
        try {
            repo.eliminar(id);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
    }

    @GET
    @Path("/search")
    public Response buscar(
            @QueryParam("titulo") String titulo,
            @QueryParam("autor") String autor,
            @QueryParam("year") Integer year,
            @QueryParam("month") Integer month,
            @QueryParam("day") Integer day) {

        if (titulo == null && autor == null && year == null)
            return error(Response.Status.BAD_REQUEST, "Cal indicar titulo, autor o year");

        List<Video> resultats;
        if (titulo != null)       resultats = repo.buscarPorTitulo(titulo);
        else if (autor != null)   resultats = repo.buscarPorAutor(autor);
        else                      resultats = repo.buscarPorFecha(year, month, day);

        return Response.ok(resultats).build();
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(status.getStatusCode(), message))
                .build();
    }
}
