package com.tejas.bankpaymentservice.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.PaymentStatus;
import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.bankpaymentservice.configurations.CallbackUrl;
import com.tejas.bankpaymentservice.models.Payment;
import com.tejas.bankpaymentservice.models.PaymentResponse;
import com.tejas.bankpaymentservice.repositories.PaymentRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentConsumer {
    private final PaymentRepo repo;
    private final CallbackUrl callbackUrls;
    private final RestTemplate template;
    
	@KafkaListener(topics = "payment-message-topic", groupId = "banking-system-simulator-group")
	public void messageReceiver(TransactionEvent trEvent) {
		Payment payment = repo.getById(trEvent.getPaymentId());
		
		if (payment != null) {
			String url = callbackUrls.getCallbackUrl().get(payment.getVendorId());
			
			PaymentResponse response = PaymentResponse.builder()
					.paymentId(trEvent.getPaymentId())
					.build();
			
			if (trEvent.getStatus().equals(TransactionStatus.CREDIT_SUCCESS)) {
				response.setStatus(PaymentStatus.SUCCESS);
				response.setMessage("Payment successful.");
				
				payment.setStatus(PaymentStatus.SUCCESS);
			} else if (trEvent.getStatus().equals(TransactionStatus.CREDIT_FAILED)) {
				response.setStatus(PaymentStatus.FAILED);
				response.setMessage("Credit failed repay process initiated.");
				
				payment.setStatus(PaymentStatus.FAILED);
			} else if (trEvent.getStatus().equals(TransactionStatus.INSUFFICIENT_BALANCE)) {
				response.setStatus(PaymentStatus.INSUFFICIENT_BALANCE);
				response.setMessage("Payment failed due to insufficient balance.");
				
				payment.setStatus(PaymentStatus.INSUFFICIENT_BALANCE);
			} else {
				response.setStatus(PaymentStatus.FAILED);
				response.setMessage("Payment failed.");
				
				payment.setStatus(PaymentStatus.FAILED);
			}

			repo.save(payment);
			
			template.postForObject(url, response, String.class);
		}
	}
}
