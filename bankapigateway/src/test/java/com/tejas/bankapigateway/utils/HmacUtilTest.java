package com.tejas.bankapigateway.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HmacUtilTest {

    @Test
    void testHmacSha256_Success() {
        String secret = "secret123";
        String message = "test message";
        
        String result = HmacUtil.hmacSha256(secret, message);
        
        assertNotNull(result);
        assertEquals(44, result.length());
    }

    @Test
    void testHmacSha256_EmptyMessage() {
        String secret = "secret123";
        String message = "";
        
        String result = HmacUtil.hmacSha256(secret, message);
        
        assertNotNull(result);
        assertEquals(44, result.length());
    }

    @Test
    void testHmacSha256_EmptySecret() {
        String secret = "";
        String message = "test message";
        
        assertThrows(RuntimeException.class, () -> HmacUtil.hmacSha256(secret, message));
    }

    @Test
    void testHmacSha256_ConsistentResults() {
        String secret = "secret123";
        String message = "test message";
        
        String result1 = HmacUtil.hmacSha256(secret, message);
        String result2 = HmacUtil.hmacSha256(secret, message);
        
        assertEquals(result1, result2);
    }

    @Test
    void testHmacSha256_DifferentSecrets() {
        String secret1 = "secret123";
        String secret2 = "secret456";
        String message = "test message";
        
        String result1 = HmacUtil.hmacSha256(secret1, message);
        String result2 = HmacUtil.hmacSha256(secret2, message);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertFalse(result1.equals(result2));
    }

    @Test
    void testHmacSha256_DifferentMessages() {
        String secret = "secret123";
        String message1 = "test message 1";
        String message2 = "test message 2";
        
        String result1 = HmacUtil.hmacSha256(secret, message1);
        String result2 = HmacUtil.hmacSha256(secret, message2);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertFalse(result1.equals(result2));
    }

    @Test
    void testHmacSha256_LongMessage() {
        String secret = "secret123";
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("a");
        }
        
        String result = HmacUtil.hmacSha256(secret, longMessage.toString());
        
        assertNotNull(result);
        assertEquals(44, result.length());
    }

    @Test
    void testHmacSha256_SpecialCharacters() {
        String secret = "secret!@#$%^&*()";
        String message = "message with special chars: !@#$%^&*()";
        
        String result = HmacUtil.hmacSha256(secret, message);
        
        assertNotNull(result);
        assertEquals(44, result.length());
    }

    @Test
    void testHmacSha256_UnicodeCharacters() {
        String secret = "secret123";
        String message = "message with unicode: 你好世界 🌍";
        
        String result = HmacUtil.hmacSha256(secret, message);
        
        assertNotNull(result);
        assertEquals(44, result.length());
    }

    @Test
    void testHmacSha256_Base64Encoded() {
        String secret = "secret123";
        String message = "test message";
        
        String result = HmacUtil.hmacSha256(secret, message);
        
        assertNotNull(result);
        assertTrue(result.matches("^[A-Za-z0-9+/=]+$"));
    }
}
