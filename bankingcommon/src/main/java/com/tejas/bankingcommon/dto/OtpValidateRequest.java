package com.tejas.bankingcommon.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpValidateRequest {
	@NotNull
	private String referenceId;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	private MessageType type;
	
	@NotNull
	private long otpValue;
	
}