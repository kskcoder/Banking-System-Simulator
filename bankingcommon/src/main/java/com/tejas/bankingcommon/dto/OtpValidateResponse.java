package com.tejas.bankingcommon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpValidateResponse {
	
	@NotNull
	private String referenceId;
	
	@NotNull
	private boolean validated;
	
	@NotNull
	private boolean retried;
	
	@NotNull
	private String message;
	
}