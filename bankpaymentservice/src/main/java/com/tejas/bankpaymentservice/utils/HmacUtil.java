package com.tejas.bankpaymentservice.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public static String hmacSha256(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash); 
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC", e);
        }
    }
}

