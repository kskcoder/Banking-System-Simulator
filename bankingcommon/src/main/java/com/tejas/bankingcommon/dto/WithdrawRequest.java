package com.tejas.bankingcommon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cash withdrawal request")
public class WithdrawRequest {
	@NotNull
	@Schema(description = "Account number to withdraw from", example = "AC123456789", required = true)
	private String accountNumber;
	
	@NotNull
	@Schema(description = "Amount to withdraw", example = "1000.0", required = true)
	private double amount;
}


