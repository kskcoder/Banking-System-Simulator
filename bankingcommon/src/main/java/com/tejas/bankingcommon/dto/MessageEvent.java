package com.tejas.bankingcommon.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageEvent {
	@NotNull
	private String otpNumber;
	@NotNull
	private String email;
	@NotNull
	@Enumerated(EnumType.STRING)
	private MessageType type;
	
	private String message; 
}
