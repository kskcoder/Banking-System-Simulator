package com.tejas.bankapigateway.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tejas.bankapigateway.configurations.ExternalVendorSecretsConfig;
import com.tejas.bankapigateway.utils.HmacUtil;

@ExtendWith(MockitoExtension.class)
class VendorValidationTest {

    @Mock
    private ExternalVendorSecretsConfig externalConfig;

    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private VendorValidation vendorValidation;

    private String vendorId;
    private String vendorSecret;
    private String timestamp;
    private String body;
    private Map<String, String> vendorKeyMap;

    @BeforeEach
    void setUp() {
        vendorId = "vendor1";
        vendorSecret = "secret123";
        timestamp = "1234567890";
        body = "{\"amount\":100,\"accountId\":1}";
        
        vendorKeyMap = new HashMap<>();
        vendorKeyMap.put(vendorId, vendorSecret);
        
        when(externalConfig.getVendorKey()).thenReturn(vendorKeyMap);
        
        vendorValidation = new VendorValidation(externalConfig, objectMapper);
    }

    @Test
    void testVerifySignature_Success() {
        String normalizedBody = "{\"amount\":100.0,\"accountId\":1}";
        String message = normalizedBody + timestamp;
        String expectedSignature = HmacUtil.hmacSha256(vendorSecret, message);

        boolean result = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, body, expectedSignature);

        assertTrue(result);
    }

    @Test
    void testVerifySignature_InvalidSignature() {
        String normalizedBody = "{\"amount\":100.0,\"accountId\":1}";
        String message = normalizedBody + timestamp;
        String expectedSignature = HmacUtil.hmacSha256(vendorSecret, message);
        String invalidSignature = "invalid_signature";

        boolean result = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, body, invalidSignature);

        assertFalse(result);
    }

    @Test
    void testVerifySignature_VendorSecretMismatch() {
        String wrongSecret = "wrong_secret";
        String normalizedBody = "{\"amount\":100.0,\"accountId\":1}";
        String message = normalizedBody + timestamp;
        String signature = HmacUtil.hmacSha256(wrongSecret, message);

        boolean result = vendorValidation.verifySignature(vendorId, wrongSecret, timestamp, body, signature);

        assertFalse(result);
    }

    @Test
    void testVerifySignature_VendorNotFound() {
        String unknownVendorId = "unknown_vendor";
        String normalizedBody = "{\"amount\":100.0,\"accountId\":1}";
        String message = normalizedBody + timestamp;
        String signature = HmacUtil.hmacSha256(vendorSecret, message);

        boolean result = vendorValidation.verifySignature(unknownVendorId, vendorSecret, timestamp, body, signature);

        assertFalse(result);
    }

    @Test
    void testVerifySignature_NullVendorSecret() {
        vendorKeyMap.put(vendorId, null);
        
        String normalizedBody = "{\"amount\":100.0,\"accountId\":1}";
        String message = normalizedBody + timestamp;
        String signature = HmacUtil.hmacSha256(vendorSecret, message);

        boolean result = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, body, signature);

        assertFalse(result);
    }

    @Test
    void testVerifySignature_EmptyBody() {
        String emptyBody = "";
        String normalizedBody = "null";
        String message = normalizedBody + timestamp;
        String expectedSignature = HmacUtil.hmacSha256(vendorSecret, message);

        boolean result = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, emptyBody, expectedSignature);

        assertTrue(result);
    }

    @Test
    void testVerifySignature_ComplexJsonBody() {
        String complexBody = "{\"amount\":100,\"accountId\":1,\"metadata\":{\"source\":\"api\",\"timestamp\":1234567890}}";
        String normalizedBody = "{\"amount\":100.0,\"accountId\":1,\"metadata\":{\"source\":\"api\",\"timestamp\":1234567890}}";
        String message = normalizedBody + timestamp;
        String expectedSignature = HmacUtil.hmacSha256(vendorSecret, message);

        boolean result = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, complexBody, expectedSignature);

        assertTrue(result);
    }

    @Test
    void testVerifySignature_DifferentTimestamp() {
        String differentTimestamp = "9876543210";
        String normalizedBody = "{\"amount\":100.0,\"accountId\":1}";
        String message = normalizedBody + differentTimestamp;
        String expectedSignature = HmacUtil.hmacSha256(vendorSecret, message);

        boolean result = vendorValidation.verifySignature(vendorId, vendorSecret, differentTimestamp, body, expectedSignature);

        assertTrue(result);
    }

    @Test
    void testVerifySignature_WithWhitespace() {
        String bodyWithWhitespace = "{\"amount\" : 100 , \"accountId\" : 1 }";
        String normalizedBody = "{\"amount\":100.0,\"accountId\":1}";
        String message = normalizedBody + timestamp;
        String expectedSignature = HmacUtil.hmacSha256(vendorSecret, message);

        boolean result = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, bodyWithWhitespace, expectedSignature);

        assertTrue(result);
    }

    @Test
    void testVerifySignature_InvalidJson() {
        String invalidJson = "{invalid json}";
        String normalizedBody = invalidJson;
        String message = normalizedBody + timestamp;
        String wrongSignature = "wrong_signature";

        boolean result = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, invalidJson, wrongSignature);

        assertFalse(result);
    }

    @Test
    void testVerifySignature_NullBody() {
        String normalizedBody = "null";
        String message = normalizedBody + timestamp;
        String expectedSignature = HmacUtil.hmacSha256(vendorSecret, message);

        boolean result = vendorValidation.verifySignature(vendorId, vendorSecret, timestamp, null, expectedSignature);

        assertTrue(result);
    }
}
