package com.tejas.bankpaymentservice.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Payment initiation request")
public class InitiatePaymentDTO {
	@NotNull
	@Schema(description = "Vendor ID. Default: TPay. Note: Only 'TPay' is allowed for now.", example = "TPay", required = true)
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
	
	private String upiId = null;
	private int upiPin = 0;
}
