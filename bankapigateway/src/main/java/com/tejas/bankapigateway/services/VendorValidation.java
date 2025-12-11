package com.tejas.bankapigateway.services;

import org.springframework.stereotype.Service;

import com.tejas.bankapigateway.configurations.ExternalVendorSecretsConfig;
import com.tejas.bankapigateway.utils.HmacUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VendorValidation {

	private final ExternalVendorSecretsConfig externalConfig;
	
	public boolean verifySignature(String vendorId, String vendorSecret, String timestamp, String body, String signature) {
		String configVendorSecret = externalConfig.getVendorKey().get(vendorId);
		
		if (vendorSecret.equals(configVendorSecret)) {
            return false;
        }
		
		String expectedSig = HmacUtil.hmacSha256(vendorSecret, body + timestamp);

		return expectedSig.equals(signature);
	}
}
