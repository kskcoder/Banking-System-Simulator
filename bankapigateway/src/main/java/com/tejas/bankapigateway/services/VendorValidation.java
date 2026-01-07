package com.tejas.bankapigateway.services;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tejas.bankapigateway.configurations.ExternalVendorSecretsConfig;
import com.tejas.bankapigateway.utils.HmacUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorValidation {

	private final ExternalVendorSecretsConfig externalConfig;
	private final ObjectMapper objectMapper;
	
	public boolean verifySignature(String vendorId, String vendorSecret, String timestamp, String body, String signature) {
		System.out.println("Gateway VendorValidation.verifySignature - Called");
		System.out.println("Gateway VendorValidation.verifySignature - vendorId: " + vendorId);
		System.out.println("Gateway VendorValidation.verifySignature - timestamp: " + timestamp);
		System.out.println("Gateway VendorValidation.verifySignature - body length: " + (body != null ? body.length() : "null"));
		System.out.println("Gateway VendorValidation.verifySignature - body preview: " + (body != null ? body.substring(0, Math.min(100, body.length())) + "..." : "null"));
		
		String configVendorSecret = externalConfig.getVendorKey().get(vendorId);
		
		if (configVendorSecret == null || !vendorSecret.equals(configVendorSecret)) {
			System.out.println("Gateway VendorValidation.verifySignature - Vendor secret mismatch");
			return false;
		}
		
		String normalizedBody = normalizeJsonBody(body);
		System.out.println("Gateway VendorValidation.verifySignature - Normalized body: " + normalizedBody);
		System.out.println("Gateway VendorValidation.verifySignature - Message for HMAC: " + normalizedBody + timestamp);
		
		String expectedSig = HmacUtil.hmacSha256(vendorSecret, normalizedBody + timestamp);
		
		System.out.println("Gateway VendorValidation.verifySignature - Expected signature: " + expectedSig);
		System.out.println("Gateway VendorValidation.verifySignature - Received signature: " + signature);
		System.out.println("Gateway VendorValidation.verifySignature - Signatures match: " + expectedSig.equals(signature));

		return expectedSig.equals(signature);
	}
	
	private String normalizeJsonBody(String body) {
		try {
			JsonNode jsonNode = objectMapper.readTree(body);
			String normalized = objectMapper.writeValueAsString(jsonNode);
			
			normalized = normalized.replaceAll("\\s*:\\s*", ":");
			normalized = normalized.replaceAll("\\s*,\\s*", ",");
			normalized = normalized.replaceAll("\\s*\\{\\s*", "{");
			normalized = normalized.replaceAll("\\s*\\}\\s*", "}");
			normalized = normalized.replaceAll("\\s*\\[\\s*", "[");
			normalized = normalized.replaceAll("\\s*\\]\\s*", "]");
			
			normalized = normalized.replaceAll("\"amount\":(\\d+),", "\"amount\":$1.0,");
			normalized = normalized.replaceAll("\"amount\":(\\d+)\\}", "\"amount\":$1.0}");
			
			return normalized;
		} catch (Exception e) {
			log.warn("Gateway - Failed to normalize JSON body, using original: {}", e.getMessage());
			return body;
		}
	}
}
