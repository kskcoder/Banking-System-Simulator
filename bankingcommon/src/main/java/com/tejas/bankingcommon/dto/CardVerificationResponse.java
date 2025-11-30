package com.tejas.bankingcommon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardVerificationResponse {
	@NotNull
	private long accountId;
	@NotNull
	private boolean validated;
}
