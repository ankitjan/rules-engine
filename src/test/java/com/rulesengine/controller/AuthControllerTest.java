package com.rulesengine.controller;

import com.rulesengine.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginWithValidCredentialsShouldReturnToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login", loginRequest, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("accessToken"));
        assertEquals("Bearer", response.getBody().get("tokenType"));
    }

    @Test
    void loginWithInvalidCredentialsShouldReturnUnauthorized() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrongpassword");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/auth/login", loginRequest, Map.class);
            
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        } catch (Exception e) {
            // Expected for invalid credentials - test passes
            assertTrue(e.getMessage().contains("401") || e.getMessage().contains("authentication"));
        }
    }
}