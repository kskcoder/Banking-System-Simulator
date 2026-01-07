package com.tejas.bankpaymentservice.models;

import com.tejas.bankingcommon.enums.PaymentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Payment initiation request")
public class InitiatePaymentDTO {
	@NotNull
	private String toAccountNumber;
	@NotNull
	private double amount;
	@NotNull
	@Schema(description = "Payment type. Use CARD for card payments or UPI for UPI payments.", example = "CARD", required = true)
	private PaymentType type;
	
	private String cardNumber;
	private String cvv;
	private String expiry;
	
	@Schema(description = "UPI ID for UPI payments. Default: null", nullable = true)
	private String upiId;
	@Schema(description = "UPI PIN for UPI payments. Default: 0", example = "0")
	private int upiPin;
}
