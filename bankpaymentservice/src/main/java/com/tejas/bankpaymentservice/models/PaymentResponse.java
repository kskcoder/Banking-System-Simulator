package com.tejas.bankpaymentservice.models;

import com.tejas.bankingcommon.enums.PaymentStatus;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PaymentResponse {
	private long paymentId;
	
	@Enumerated(EnumType.STRING)
	private PaymentStatus status;
}
