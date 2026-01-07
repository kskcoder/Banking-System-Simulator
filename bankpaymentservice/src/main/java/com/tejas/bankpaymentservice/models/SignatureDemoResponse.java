package com.tejas.bankpaymentservice.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureDemoResponse {
	private String signature;
	private String timestamp;
	private String body;
	private String vendorId;
	private String vendorSecret;
}

