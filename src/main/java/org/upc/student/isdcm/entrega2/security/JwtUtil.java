package org.upc.student.isdcm.entrega2.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class JwtUtil {

    private static final String SECRET = "isdcm-entrega3-jwt-secret-please-change-me-32bytes!!";
    private static final byte[] SECRET_BYTES = SECRET.getBytes(StandardCharsets.UTF_8);
    private static final String ISSUER = "isdcm-backend";
    private static final long TTL_MILLIS = 60L * 60L * 1000L;

    private JwtUtil() {}

    public static String createToken(String username, String apiKey) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setIssuer(ISSUER)
                .setSubject(username)
                .claim("apiKey", apiKey)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TTL_MILLIS))
                .signWith(SignatureAlgorithm.HS256, SECRET_BYTES)
                .compact();
    }

    public static String getSecret() {
        return SECRET;
    }
}
