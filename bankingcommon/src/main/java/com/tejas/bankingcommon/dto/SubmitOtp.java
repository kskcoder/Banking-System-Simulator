package com.tejas.bankingcommon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitOtp {
	@NotNull
	private long paymentId;
	@NotNull
	private int otp;
}
