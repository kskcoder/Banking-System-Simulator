package com.tejas.bankingcommon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OTP validation request")
public class OtpValidateRequest {
	@NotNull
	@Schema(description = "Reference ID from OTP request", example = "12345", required = true)
	private String referenceId;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	@Schema(description = "Message type", example = "PAYMENT", required = true)
	private MessageType type;
	
	@NotNull
	@Schema(description = "OTP value to validate", example = "123456", required = true)
	private String otpValue;
	
}