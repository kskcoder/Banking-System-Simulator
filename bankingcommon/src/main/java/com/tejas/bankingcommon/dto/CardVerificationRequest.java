package com.tejas.bankingcommon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Card verification request")
public class CardVerificationRequest {
	@NotNull
	@Schema(description = "Card number", example = "1234567890123456", required = true)
	private String cardNumber;
	
	@NotNull
	@Schema(description = "CVV code", example = "123", required = true)
	private String cvv;
	
	@NotNull
	@Schema(description = "Card expiry date", example = "12/25", required = true)
	private String expiryDate;
	
	@NotNull
	@Schema(description = "Transaction amount", example = "100.0", required = true)
	private double amount;
}
