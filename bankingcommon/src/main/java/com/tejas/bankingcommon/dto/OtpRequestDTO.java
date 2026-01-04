package com.tejas.bankingcommon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "OTP request details")
public class OtpRequestDTO {
	@NotNull
	@Schema(description = "Reference ID for the transaction", example = "12345", required = true)
	private long referenceId;
	
	@NotNull
	@Schema(description = "Message type for OTP", example = "PAYMENT", required = true)
	private MessageType type;
	
	@NotNull
	@Schema(description = "User ID", example = "1", required = true)
	private long userId;
}
