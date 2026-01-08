package com.tejas.bankapigateway.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class JWTServiceTest {

    @InjectMocks
    private JWTService jwtService;

    private String validSecretKey;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            keyBytes[i] = (byte) i;
        }
        validSecretKey = Base64.getEncoder().encodeToString(keyBytes);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
        
        ReflectionTestUtils.setField(jwtService, "secretKey", validSecretKey);
    }

    @Test
    void testExtractUserId_Success() {
        String userId = "123";
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();

        String result = jwtService.extractUserId(token);

        assertEquals(userId, result);
    }

    @Test
    void testExtractUserId_InvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThrows(Exception.class, () -> jwtService.extractUserId(invalidToken));
    }

    @Test
    void testExtractUserId_ExpiredToken() {
        String userId = "123";
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(secretKey)
                .compact();

        assertThrows(Exception.class, () -> jwtService.extractUserId(token));
    }

    @Test
    void testExtractAllClaims_Success() {
        String userId = "123";
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        claims.put("email", "test@example.com");
        
        String token = Jwts.builder()
                .subject(userId)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();

        Claims result = jwtService.extractAllClaims(token);

        assertNotNull(result);
        assertEquals(userId, result.getSubject());
        assertEquals("USER", result.get("role"));
        assertEquals("test@example.com", result.get("email"));
    }

    @Test
    void testExtractAllClaims_InvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThrows(Exception.class, () -> jwtService.extractAllClaims(invalidToken));
    }

    @Test
    void testExtractAllClaims_WrongSecret() {
        String userId = "123";
        byte[] wrongKeyBytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            wrongKeyBytes[i] = (byte) (i + 1);
        }
        SecretKey wrongSecretKey = Keys.hmacShaKeyFor(wrongKeyBytes);
        
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongSecretKey)
                .compact();

        assertThrows(Exception.class, () -> jwtService.extractAllClaims(token));
    }

    @Test
    void testExtractUserId_EmptyToken() {
        String emptyToken = "";

        assertThrows(Exception.class, () -> jwtService.extractUserId(emptyToken));
    }

    @Test
    void testExtractUserId_NullToken() {
        assertThrows(Exception.class, () -> jwtService.extractUserId(null));
    }
}
