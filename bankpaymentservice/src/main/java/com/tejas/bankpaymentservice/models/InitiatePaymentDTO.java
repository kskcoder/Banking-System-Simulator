package com.tejas.bankpaymentservice.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitiatePaymentDTO {
	@NotNull
	private String vendorId;
	@NotNull
	private String fromAccountNumber;
	@NotNull
	private String toAccountNumber;
	@NotNull
	private double amount;
	@NotNull
	private int type;
	
	private String cardNumber;
	private int cvv;
	private String expiry;
	
	private String upiId;
	private int upiPin;
}
