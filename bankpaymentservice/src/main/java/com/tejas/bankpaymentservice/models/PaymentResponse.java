package com.tejas.bankpaymentservice.models;

import com.tejas.bankingcommon.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
	private long paymentId;
	
	private PaymentStatus status;
	
	private String message;
}
