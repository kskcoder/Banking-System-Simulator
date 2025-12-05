package com.tejas.bankingcommon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpValidateRequest {
	@NotNull
	private long referenceId;
	
	@NotNull
	private MessageType type;
	
	@NotNull
	private long otpValue;
	
}