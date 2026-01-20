package com.rulesengine.controller;

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
class HealthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointShouldReturnUp() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/health", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("rules-engine", response.getBody().get("service"));
    }

    @Test
    void readinessEndpointShouldReturnReady() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/health/ready", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("READY", response.getBody().get("status"));
    }

    @Test
    void livenessEndpointShouldReturnAlive() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/health/live", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ALIVE", response.getBody().get("status"));
    }
}