package com.tejas.bankingcommon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserContactDetails {
	@NotNull
	private String email;
	
	@NotNull
	private String phone;
}
