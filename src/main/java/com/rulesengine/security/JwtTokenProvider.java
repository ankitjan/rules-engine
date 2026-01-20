package com.rulesengine.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${spring.security.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.security.jwt.expiration}")
    private int jwtExpirationInMs;

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        
        long now = System.currentTimeMillis();
        long expiryTime = now + jwtExpirationInMs;
        
        // Simple JWT implementation for demo purposes
        String header = Base64.getEncoder().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getEncoder().encodeToString(
            String.format("{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}", 
                userPrincipal.getUsername(), now / 1000, expiryTime / 1000).getBytes()
        );
        
        String signature = createSignature(header + "." + payload);
        
        return header + "." + payload + "." + signature;
    }

    public String getUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            // Simple extraction - in production use proper JSON parsing
            String sub = payload.substring(payload.indexOf("\"sub\":\"") + 7);
            sub = sub.substring(0, sub.indexOf("\""));
            return sub;
        } catch (Exception e) {
            logger.error("Error extracting username from token", e);
            return null;
        }
    }

    public boolean validateToken(String authToken) {
        try {
            String[] parts = authToken.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            String expectedSignature = createSignature(parts[0] + "." + parts[1]);
            if (!expectedSignature.equals(parts[2])) {
                return false;
            }
            
            // Check expiration
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            String expStr = payload.substring(payload.indexOf("\"exp\":") + 6);
            expStr = expStr.substring(0, expStr.indexOf(",") > 0 ? expStr.indexOf(",") : expStr.indexOf("}"));
            long exp = Long.parseLong(expStr);
            
            return exp > (System.currentTimeMillis() / 1000);
        } catch (Exception ex) {
            logger.error("JWT validation failed", ex);
            return false;
        }
    }
    
    private String createSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error creating JWT signature", e);
        }
    }
}