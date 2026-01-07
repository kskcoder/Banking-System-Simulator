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
		String configVendorSecret = externalConfig.getVendorKey().get(vendorId);
		
		if (configVendorSecret == null || !vendorSecret.equals(configVendorSecret)) {
			return false;
		}
		
		String normalizedBody = normalizeJsonBody(body);
		String expectedSig = HmacUtil.hmacSha256(vendorSecret, normalizedBody + timestamp);

		return expectedSig.equals(signature);
	}
	
	private String normalizeJsonBody(String body) {
		try {
			JsonNode jsonNode = objectMapper.readTree(body);
			String normalized = objectMapper.writeValueAsString(jsonNode);
			
			normalized = normalized.replaceAll("\"amount\":(\\d+),", "\"amount\":$1.0,");
			normalized = normalized.replaceAll("\"amount\":(\\d+)\\}", "\"amount\":$1.0}");
			
			return normalized;
		} catch (Exception e) {
			log.warn("Gateway - Failed to normalize JSON body, using original: {}", e.getMessage());
			return body;
		}
	}
}
