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
public class TransferRequest {
	@NotNull
	private String fromAccount;
	@NotNull
    private String toAccount;
	@NotNull
    private double amount;
	
	@Schema(hidden = true)
	private long paymentId;
}