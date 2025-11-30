package com.tejas.bankingcommon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardVerificationDTO {
	@NotNull
	private String cardNumber;
	@NotNull
	private String cvv;
	@NotNull
	private String expiryDate;
	@NotNull
	private double amount;
}
