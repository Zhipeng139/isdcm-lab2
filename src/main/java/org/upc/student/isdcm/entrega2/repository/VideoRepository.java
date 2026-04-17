package org.upc.student.isdcm.entrega2.repository;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.upc.student.isdcm.entrega2.model.DatabaseProvider;
import org.upc.student.isdcm.entrega2.model.Video;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VideoRepository {

    @PostConstruct
    public void init() {
        try {
            initializeTable();
        } catch (SQLException e) {
            throw new RuntimeException("Error inicialitzant la taula videos", e);
        }
    }

    private void initializeTable() throws SQLException {
        String sql = """
                CREATE TABLE videos (
                    id VARCHAR(80) PRIMARY KEY,
                    titulo VARCHAR(150) NOT NULL,
                    autor VARCHAR(100) NOT NULL,
                    fecha_creacion DATE NOT NULL,
                    duracion INT NOT NULL,
                    reproducciones INT NOT NULL,
                    descripcion VARCHAR(800) NOT NULL,
                    formato VARCHAR(40) NOT NULL,
                    url VARCHAR(400) NOT NULL,
                    categoria VARCHAR(120) NOT NULL,
                    resolucion VARCHAR(40) NOT NULL
                )
                """;
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (!"X0Y32".equals(e.getSQLState())) throw e;
        }
    }

    public List<Video> findAll() {
        String sql = "SELECT * FROM videos ORDER BY fecha_creacion DESC";
        List<Video> videos = new ArrayList<>();
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) videos.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return videos;
    }

    public Video findById(String id) {
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM videos WHERE id = ?")) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Video crear(Video video) {
        String sql = "INSERT INTO videos (id, titulo, autor, fecha_creacion, duracion, reproducciones, descripcion, formato, url, categoria, resolucion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, video.getId());
            stmt.setString(2, video.getTitulo());
            stmt.setString(3, video.getAutor());
            stmt.setDate(4, Date.valueOf(video.getFechaCreacion()));
            stmt.setInt(5, video.getDuracion());
            stmt.setInt(6, video.getReproducciones());
            stmt.setString(7, video.getDescripcion());
            stmt.setString(8, video.getFormato());
            stmt.setString(9, video.getUrl());
            stmt.setString(10, video.getCategoria());
            stmt.setString(11, video.getResolucion());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()))
                throw new IllegalArgumentException("Video with this id already exists");
            throw new RuntimeException(e);
        }
        return video;
    }

    public void eliminar(String id) {
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM videos WHERE id = ?")) {
            stmt.setString(1, id);
            if (stmt.executeUpdate() == 0)
                throw new IllegalArgumentException("Video not found: " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Video actualizar(Video video) {
        String sql = "UPDATE videos SET titulo=?, autor=?, fecha_creacion=?, duracion=?, reproducciones=?, descripcion=?, formato=?, url=?, categoria=?, resolucion=? WHERE id=?";
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, video.getTitulo());
            stmt.setString(2, video.getAutor());
            stmt.setDate(3, Date.valueOf(video.getFechaCreacion()));
            stmt.setInt(4, video.getDuracion());
            stmt.setInt(5, video.getReproducciones());
            stmt.setString(6, video.getDescripcion());
            stmt.setString(7, video.getFormato());
            stmt.setString(8, video.getUrl());
            stmt.setString(9, video.getCategoria());
            stmt.setString(10, video.getResolucion());
            stmt.setString(11, video.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return video;
    }

    public void incrementarReproducciones(String id) {
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE videos SET reproducciones = reproducciones + 1 WHERE id = ?")) {
            stmt.setString(1, id);
            if (stmt.executeUpdate() == 0)
                throw new IllegalArgumentException("Video no encontrado: " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Video> buscarPorTitulo(String titulo) {
        return buscarConParam("SELECT * FROM videos WHERE LOWER(titulo) LIKE LOWER(?)", "%" + titulo + "%");
    }

    public List<Video> buscarPorAutor(String autor) {
        return buscarConParam("SELECT * FROM videos WHERE LOWER(autor) LIKE LOWER(?)", "%" + autor + "%");
    }

    public List<Video> buscarPorFecha(Integer year, Integer month, Integer day) {
        String sql;
        List<Integer> params = new ArrayList<>();
        if (day != null && month != null) {
            sql = "SELECT * FROM videos WHERE YEAR(fecha_creacion) = ? AND MONTH(fecha_creacion) = ? AND DAY(fecha_creacion) = ?";
            params.add(year); params.add(month); params.add(day);
        } else if (month != null) {
            sql = "SELECT * FROM videos WHERE YEAR(fecha_creacion) = ? AND MONTH(fecha_creacion) = ?";
            params.add(year); params.add(month);
        } else {
            sql = "SELECT * FROM videos WHERE YEAR(fecha_creacion) = ?";
            params.add(year);
        }
        List<Video> videos = new ArrayList<>();
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) stmt.setInt(i + 1, params.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) videos.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return videos;
    }

    private List<Video> buscarConParam(String sql, String param) {
        List<Video> videos = new ArrayList<>();
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) videos.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return videos;
    }

    private Video mapRow(ResultSet rs) throws SQLException {
        Video v = new Video();
        v.setId(rs.getString("id"));
        v.setTitulo(rs.getString("titulo"));
        v.setAutor(rs.getString("autor"));
        v.setFechaCreacion(rs.getDate("fecha_creacion").toLocalDate());
        v.setDuracion(rs.getInt("duracion"));
        v.setReproducciones(rs.getInt("reproducciones"));
        v.setDescripcion(rs.getString("descripcion"));
        v.setFormato(rs.getString("formato"));
        v.setUrl(rs.getString("url"));
        v.setCategoria(rs.getString("categoria"));
        v.setResolucion(rs.getString("resolucion"));
        return v;
    }
}
