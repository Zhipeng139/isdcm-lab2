package org.upc.student.isdcm.entrega2.repository;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.upc.student.isdcm.entrega2.model.DatabaseProvider;
import org.upc.student.isdcm.entrega2.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.UUID;

@ApplicationScoped
public class UsuarioRepository {

    @PostConstruct
    public void init() {
        try {
            initializeTable();
        } catch (SQLException e) {
            throw new RuntimeException("Error inicialitzant la taula usuaris", e);
        }
    }

    private void initializeTable() throws SQLException {
        String createSQL = """
                CREATE TABLE usuaris (
                    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    nombre VARCHAR(80) NOT NULL,
                    apellido VARCHAR(80) NOT NULL,
                    email VARCHAR(120) NOT NULL,
                    username VARCHAR(50) NOT NULL,
                    password_hash VARCHAR(64) NOT NULL,
                    api_key VARCHAR(36) NOT NULL,
                    CONSTRAINT uq_username UNIQUE (username),
                    CONSTRAINT uq_email UNIQUE (email),
                    CONSTRAINT uq_api_key UNIQUE (api_key)
                )
                """;
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(createSQL)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (!"X0Y32".equals(e.getSQLState())) throw e;
            // Table exists: add api_key column if missing (upgrade from older schema)
            try (Connection conn = DatabaseProvider.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "ALTER TABLE usuaris ADD COLUMN api_key VARCHAR(36) NOT NULL DEFAULT 'legacy'")) {
                stmt.executeUpdate();
            } catch (SQLException e2) { /* column already exists, ignore */ }
        }
    }

    public String createUser(Usuario u) {
        if (existsByUsername(u.getUsername()))
            throw new IllegalArgumentException("Username already taken");
        if (existsByEmail(u.getEmail()))
            throw new IllegalArgumentException("Email already taken");

        String apiKey = UUID.randomUUID().toString();
        String sql = "INSERT INTO usuaris (nombre, apellido, email, username, password_hash, api_key) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, u.getNombre().toLowerCase());
            stmt.setString(2, u.getApellido().toLowerCase());
            stmt.setString(3, u.getEmail().toLowerCase());
            stmt.setString(4, u.getUsername().toLowerCase());
            stmt.setString(5, hash(u.getPassword()));
            stmt.setString(6, apiKey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return apiKey;
    }

    public String getApiKey(String username) {
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT api_key FROM usuaris WHERE username = ?")) {
            stmt.setString(1, username.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("api_key") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean validateApiKey(String apiKey) {
        return exists("SELECT 1 FROM usuaris WHERE api_key = ?", apiKey);
    }

    public boolean validateCredentials(String username, String password) {
        String sql = "SELECT 1 FROM usuaris WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.toLowerCase());
            stmt.setString(2, hash(password));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean existsByUsername(String username) {
        return exists("SELECT 1 FROM usuaris WHERE username = ?", username.toLowerCase());
    }

    private boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM usuaris WHERE email = ?", email.toLowerCase());
    }

    private boolean exists(String sql, String param) {
        try (Connection conn = DatabaseProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
