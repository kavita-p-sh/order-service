package com.ecommerce.order.config;

import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Provides JWT secret key for token generation and validation.
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Converts secret into SecretKey.
     */
    @Bean
    public SecretKey secretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return new SecretKeySpec(keyBytes, "AES");
    }
}