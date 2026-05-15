package org.upc.student.isdcm.entrega2.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.OctetSequenceJsonWebKey;
import org.jose4j.keys.AesKey;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    private static final String JWE_SECRET = "isdcm-entrega3-jwe-key-32bytes!!";
    private static final byte[] JWE_KEY_BYTES = sha256Bytes(JWE_SECRET);

    public static String wrapInJwe(String jws) {
        try {
            JsonWebEncryption jwe = new JsonWebEncryption();
            jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW);
            jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
            jwe.setHeader("cty", "JWT");
            jwe.setKey(new AesKey(java.util.Arrays.copyOf(JWE_KEY_BYTES, 16)));
            jwe.setPayload(jws);
            return jwe.getCompactSerialization();
        } catch (Exception e) {
            throw new IllegalStateException("Error generando JWE: " + e.getMessage(), e);
        }
    }

    public static String unwrapJwe(String compactJwe) {
        try {
            JsonWebEncryption jwe = new JsonWebEncryption();
            jwe.setAlgorithmConstraints(new AlgorithmConstraints(
                    AlgorithmConstraints.ConstraintType.PERMIT,
                    KeyManagementAlgorithmIdentifiers.A128KW));
            jwe.setContentEncryptionAlgorithmConstraints(new AlgorithmConstraints(
                    AlgorithmConstraints.ConstraintType.PERMIT,
                    ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256));
            jwe.setKey(new AesKey(java.util.Arrays.copyOf(JWE_KEY_BYTES, 16)));
            jwe.setCompactSerialization(compactJwe);
            return jwe.getPayload();
        } catch (Exception e) {
            throw new IllegalStateException("Error descifrando JWE: " + e.getMessage(), e);
        }
    }

    public static String jweKeyBase64Url() {
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(java.util.Arrays.copyOf(JWE_KEY_BYTES, 16));
    }

    public static String jweKeyAsJwk() {
        try {
            OctetSequenceJsonWebKey jwk = new OctetSequenceJsonWebKey(
                    new SecretKeySpec(java.util.Arrays.copyOf(JWE_KEY_BYTES, 16), "AES"));
            jwk.setKeyId("isdcm-jwe-1");
            return jwk.toJson(JsonWebKey.OutputControlLevel.INCLUDE_SYMMETRIC);
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private static byte[] sha256Bytes(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
