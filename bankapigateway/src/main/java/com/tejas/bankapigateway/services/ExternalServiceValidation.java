package com.tejas.bankapigateway.services;

import org.springframework.stereotype.Service;
import com.tejas.bankapigateway.configurations.ExternalServiceSercretsConfig;
import com.tejas.bankapigateway.utils.HmacUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalServiceValidation {

	private final ExternalServiceSercretsConfig externalConfig;
	private String vendorSecret;
	
	public boolean verifySignature(String vendorId, String timestamp, String body, String signature) {
		this.vendorSecret = externalConfig.getExternal().get(vendorId);
		
		if (vendorSecret == null) {
            return false;
        }
		
		String expectedSig = HmacUtil.hmacSha256(vendorSecret, body + timestamp);

		return expectedSig.equals(signature);
	}
}
