package com.ecommerce.order.security;

import com.ecommerce.common.util.JwtConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class for JWT operations in order-service.
 * Order-service only validates token and extracts data.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Extracts username from token.
     */
    public String extractUsername(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }

    /**
     * Extracts role from token.
     */
    public String extractRole(String token) {
        Claims claims = extractClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Validates JWT token without UserDetailsService.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getSubject() != null
                    && claims.getExpiration() != null
                    && !claims.getExpiration().before(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Extracts all claims from encrypted JWT token.
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .decryptWith(secretKey)
                .build()
                .parseEncryptedClaims(getToken(token))
                .getPayload();
    }

    /**
     * Removes Bearer prefix from token if present.
     */
    private String getToken(String token) {
        if (token != null && token.startsWith(JwtConstant.TOKEN_PREFIX)) {
            return token.substring(JwtConstant.TOKEN_PREFIX.length()).trim();
        }
        return token;
    }
}