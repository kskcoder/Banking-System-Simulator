package com.tejas.bankpaymentservice.services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.TransactionEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentConsumer {
    private final PaymentService service;
    
	@KafkaListener(topics = "payment-message-topic", groupId = "banking-system-simulator-group")
	public void messageReceiver(TransactionEvent trEvent) {

	}
}
