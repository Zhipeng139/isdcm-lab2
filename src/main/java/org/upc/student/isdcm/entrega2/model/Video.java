package org.upc.student.isdcm.entrega2.model;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @Column(name = "id", length = 80, nullable = false)
    private String id;

    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    @Column(name = "autor", length = 100, nullable = false)
    private String autor;

    @Column(name = "fecha_creacion", nullable = false)
    @JsonbDateFormat("yyyy-MM-dd")
    private LocalDate fechaCreacion;

    @Column(name = "duracion", nullable = false)
    private int duracion;

    @Column(name = "reproducciones", nullable = false)
    private int reproducciones;

    @Column(name = "descripcion", length = 800, nullable = false)
    private String descripcion;

    @Column(name = "formato", length = 40, nullable = false)
    private String formato;

    @Column(name = "url", length = 400, nullable = false)
    private String url;

    @Column(name = "categoria", length = 120, nullable = false)
    private String categoria;

    @Column(name = "resolucion", length = 40, nullable = false)
    private String resolucion;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public int getDuracion() { return duracion; }
    public void setDuracion(int duracion) { this.duracion = duracion; }

    public int getReproducciones() { return reproducciones; }
    public void setReproducciones(int reproducciones) { this.reproducciones = reproducciones; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getResolucion() { return resolucion; }
    public void setResolucion(String resolucion) { this.resolucion = resolucion; }
}
