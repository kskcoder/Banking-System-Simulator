package com.tejas.bankingcommon.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpEvent {
	@NotNull
	private String otpNumber;
	@NotNull
	private String email;
	@NotNull
	@Enumerated(EnumType.STRING)
	private OtpType type;
	@NotNull
	private String message; 
}
