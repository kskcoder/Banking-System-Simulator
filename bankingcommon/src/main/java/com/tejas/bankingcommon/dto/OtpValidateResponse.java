package com.tejas.bankingcommon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpValidateResponse {
	
	@NotNull
	private long referenceId;
	
	@NotNull
	private boolean type;
	
	@NotNull
	private String message;
	
}